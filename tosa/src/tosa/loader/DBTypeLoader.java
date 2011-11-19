package tosa.loader;

import gw.config.BaseService;
import gw.fs.IFile;
import gw.lang.reflect.IExtendedTypeLoader;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaClassInfo;
import gw.lang.reflect.module.IExecutionEnvironment;
import gw.lang.reflect.module.IModule;
import gw.util.GosuClassUtil;
import gw.util.Pair;
import gw.util.concurrent.LockingLazyVar;
import tosa.CachedDBObject;
import tosa.api.IDBTable;
import tosa.api.IDatabase;
import tosa.dbmd.DatabaseImpl;
import tosa.loader.data.DBData;
import tosa.loader.data.IDBDataSource;
import tosa.loader.parser.DDLDBDataSource;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: alan
 * Date: 12/29/10
 * Time: 10:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class DBTypeLoader extends BaseService implements IExtendedTypeLoader {

  private IModule _module;
//  private Set<String> _initializedDrivers = new HashSet<String>();
//  private Map<String, DBConnection> _connInfos = new HashMap<String, DBConnection>();

  private LockingLazyVar<Map<String, DatabaseImpl>> _typeDataByNamespace = new LockingLazyVar<Map<String, DatabaseImpl>>() {
    protected Map<String, DatabaseImpl> init() { return initializeDBTypeData(); }
  };

  private LockingLazyVar<Map<String, SQLFileInfo>> _sqlFilesByName = new LockingLazyVar<Map<String, SQLFileInfo>>() {
    protected Map<String, SQLFileInfo> init() { return initializeSQLFiles(); }
  };

  private LockingLazyVar<Set<String>> _namespaces = new LockingLazyVar<Set<String>>() {
    protected Set<String> init() { return initializeNamespaces(); }
  };

  private LockingLazyVar<Set<String>> _typeNames = new LockingLazyVar<Set<String>>() {
    protected Set<String> init() { return initializeTypeNames(); }
  };

  public DBTypeLoader() {
    this(TypeSystem.getExecutionEnvironment(), new HashMap<String, String>());
  }

  // TODO - AHK - This should take a module and a moduleresourceaccess instead
  public DBTypeLoader(IExecutionEnvironment env, Map<String, String> args) {
    _module = env.getCurrentModule();
  }

  @Override
  public IModule getModule() {
    return _module;
  }

  @Override
  public IType getType(String fullyQualifiedName) {
    int lastDot = fullyQualifiedName.lastIndexOf('.');
    if (lastDot == -1) {
      return null;
    }
    // TODO - AHK - What do we do if there's a table named "Transaction"?
    // TODO - AHK - Is it really our job to do any caching at all?
    String namespace = fullyQualifiedName.substring(0, lastDot);
    String relativeName = fullyQualifiedName.substring(lastDot + 1);
    DatabaseImpl databaseImpl = _typeDataByNamespace.get().get(namespace);
    if (databaseImpl == null) {
      SQLFileInfo data = _sqlFilesByName.get().get(fullyQualifiedName);
      if (data != null) {
        return new SQLType(data, this).getTypeReference();
      } else {
        return null;
      }
    }

    if ("Transaction".equals(relativeName)) {
      return new TransactionType(databaseImpl, this).getTypeReference();
    } else {
      IDBTable dbTable = databaseImpl.getTable(relativeName);
      if (dbTable == null) {
        return null;
      } else {
        return new DBType(this, dbTable).getTypeReference();
      }
    }
  }

  @Override
  public Set<? extends CharSequence> getAllTypeNames() {
    return _typeNames.get();
  }

  @Override
  public Set<? extends CharSequence> getAllNamespaces() {
    return _namespaces.get();
  }

  @Override
  public URL getResource(String name) {
    throw new RuntimeException("Not supported");
//    return _module.getResource(name);
  }

  @Override
  public File getResourceFile(String name) {
    return TypeSystem.getResourceFileResolver().resolveToFile(name);
  }

  @Override
  public void refresh(boolean clearCachedTypes) {
    // TODO?
  }

  @Override
  public boolean isCaseSensitive() {
    return true;
  }

  @Override
  public List<String> getHandledPrefixes() {
    return Collections.emptyList();
  }

  @Override
  public boolean handlesNonPrefixLoads() {
    return true;
  }

  @Override
  public boolean isNamespaceOfTypeHandled(String fullyQualifiedTypeName) {
    int lastDot = fullyQualifiedTypeName.lastIndexOf('.');
    if (lastDot == -1) {
      return false;
    }
    return _typeDataByNamespace.get().keySet().contains(fullyQualifiedTypeName.substring(0, lastDot));
  }

  @Override
  public List<IType> refreshedFile(IFile iFile) {
    return null;
  }

  @Override
  public IType getIntrinsicTypeFromObject(Object object) {
    // TODO - AHK - This probably needs to work for the Transaction object as well
    if (object instanceof CachedDBObject) {
      CachedDBObject dbObj = (CachedDBObject) object;
      return dbObj.getIntrinsicType();
    } else {
      return null;
    }
  }

  private Map<String, DatabaseImpl> initializeDBTypeData() {
    IDBDataSource dataSource = new DDLDBDataSource();
    Map<String, DBData> dbDataMap = dataSource.getDBData(_module);
    Map<String, DatabaseImpl> dbTypeDataMap = new HashMap<String, DatabaseImpl>();
    for (Map.Entry<String, DBData> dbDataEntry : dbDataMap.entrySet()) {
      dbTypeDataMap.put(dbDataEntry.getKey(), new DatabaseImpl(dbDataEntry.getKey(), dbDataEntry.getValue(), this));
    }
    return dbTypeDataMap;
  }

  private Map<String, SQLFileInfo> initializeSQLFiles() {
    HashMap<String, SQLFileInfo> results = new HashMap<String, SQLFileInfo>();
    for (Pair<String, IFile> pair : _module.getFileRepository().findAllFilesByExtension(".sql")) {
      String fileName = pair.getFirst();
      IFile sqlFil = pair.getSecond();
      for (DatabaseImpl db : _typeDataByNamespace.get().values()) {
        if (sqlFil.isDescendantOf(db.getDBData().getDdlFile().getParent())) {
          String queryName = fileName.substring(0, fileName.length() - ".sql".length()).replace("/", ".");
          results.put(queryName, new SQLFileInfo(queryName, db, sqlFil));
          break;
        }
      }
    }
    return results;
  }

  private Set<String> initializeNamespaces() {
    Set<String> allNamespaces = new HashSet<String>();
    for (String namespace : _typeDataByNamespace.get().keySet()) {
      splitStringInto(allNamespaces, namespace);
    }
    for (SQLFileInfo sqlFileInfo : _sqlFilesByName.get().values()) {
      splitStringInto(allNamespaces, GosuClassUtil.getPackage(sqlFileInfo.getTypeName()));
    }
    return allNamespaces;
  }

  private void splitStringInto(Set<String> allNamespaces, String namespace) {
    String[] nsComponentsArr = namespace.split("\\.");
    for (int i = 0; i < nsComponentsArr.length; i++) {
      String nsName = "";
      for (int n = 0; n < i + 1; n++) {
        if (n > 0) {
          nsName += ".";
        }
        nsName += nsComponentsArr[n];
      }
      allNamespaces.add(nsName);
    }
  }

  private Set<String> initializeTypeNames() {
    Set<String> typeNames = new HashSet<String>();

    for (IDatabase database : _typeDataByNamespace.get().values()) {
      typeNames.add(database.getNamespace() + "." + TransactionType.TYPE_NAME);
      for (IDBTable table : database.getAllTables()) {
        if (!table.getName().contains("join_")) {
          // TODO - AHK - Should there be a utility method for converting from table to entity name?
          typeNames.add(database.getNamespace() + "." + table.getName());
        }
      }
    }
    for (SQLFileInfo sqlFileInfo : _sqlFilesByName.get().values()) {
      typeNames.add(sqlFileInfo.getTypeName());
    }
    return typeNames;
  }

  // TODO - AHK - This doesn't really belong here, but for now . . .
  public DatabaseImpl getTypeDataForNamespace(String namespace) {
    return _typeDataByNamespace.get().get(namespace);
  }

  public Collection<? extends IDatabase> getAllDatabases() {
    return _typeDataByNamespace.get().values();
  }
}
