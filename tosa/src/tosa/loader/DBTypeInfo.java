package tosa.loader;

import gw.config.CommonServices;
import gw.lang.reflect.*;
import gw.lang.reflect.features.PropertyReference;
import gw.lang.reflect.java.IJavaType;
import gw.lang.reflect.java.JavaTypes;
import gw.util.concurrent.LockingLazyVar;
import tosa.CachedDBObject;
import tosa.api.IDBArray;
import tosa.api.IDBColumn;
import tosa.api.IPreparedStatementParameter;
import tosa.db.execution.QueryExecutor;
import tosa.dbmd.DBColumnImpl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: alan
 * Date: 12/29/10
 * Time: 10:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class DBTypeInfo extends BaseTypeInfo implements ITypeInfo {

  public static final String ID_COLUMN = "id";
  private Map<String, IPropertyInfo> _properties;
  private List<IMethodInfo> _methods;
  private LockingLazyVar<Map<String, IPropertyInfo>> _arrayProperties = new LockingLazyVar<Map<String, IPropertyInfo>>() {
    @Override
    protected Map<String, IPropertyInfo> init() {
      return makeArrayProperties();
    }
  };

  private IMethodInfo _getMethod;
  private IMethodInfo _idMethod;
  private IMethodInfo _updateMethod;
  private IMethodInfo _deleteMethod;
  private IMethodInfo _countMethod;
  private IMethodInfo _countWithSqlMethod;
  private IMethodInfo _findMethod;
  private IMethodInfo _findSortedMethod;
  private IMethodInfo _findPagedMethod;
  private IMethodInfo _findSortedPagedMethod;
  private IMethodInfo _findWithSqlMethod;
  private IPropertyInfo _newProperty;
  private IConstructorInfo _ctor;
  private QueryExecutor _queryExecutor; // TODO - AHK - I'm not sure if we really want to hold onto this here

  public DBTypeInfo(IDBType dbType) {
    super(dbType);
    _queryExecutor = new QueryExecutor();

    _getMethod = new MethodInfoBuilder().withName("fromID").withStatic()
        .withParameters(new ParameterInfoBuilder().withName(ID_COLUMN).withType(JavaTypes.pLONG()))
        .withReturnType(dbType)
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.selectById(getOwnersType().getName() + ".fromID()", getOwnersType(), args[0]);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _idMethod = new MethodInfoBuilder().withName("toID")
        .withReturnType(JavaTypes.pLONG())
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            return ((CachedDBObject) ctx).getColumns().get(ID_COLUMN);
          }
        }).build(this);
    _updateMethod = new MethodInfoBuilder().withName("update")
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              ((CachedDBObject) ctx).update();
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
            return null;
          }
        }).build(this);
    _deleteMethod = new MethodInfoBuilder().withName("delete")
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              ((CachedDBObject) ctx).delete();
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
            return null;
          }
        }).build(this);
    _countWithSqlMethod = new MethodInfoBuilder().withName("countWithSql").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("sql").withType(JavaTypes.STRING()))
        .withReturnType(JavaTypes.pINT())
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.countFromSql(
                  getOwnersType().getName() + ".countWithSql()",
                  getOwnersType(),
                  (String) args[0],
                  Collections.<IPreparedStatementParameter>emptyList());
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _countMethod = new MethodInfoBuilder().withName("count").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("template").withType(dbType))
        .withReturnType(JavaTypes.pINT())
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.countFromTemplate(
                  getOwnersType().getName() + ".count()",
                  getOwnersType(),
                  (CachedDBObject) args[0]);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _findWithSqlMethod = new MethodInfoBuilder().withName("findWithSql").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("sql").withType(JavaTypes.STRING()))
        .withReturnType(JavaTypes.LIST().getGenericType().getParameterizedType(dbType))
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.findFromSql(
                  getOwnersType().getName() + ".findWithSql()",
                  getOwnersType(),
                  (String) args[0],
                  Collections.<IPreparedStatementParameter>emptyList());
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _findMethod = new MethodInfoBuilder().withName("find").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("template").withType(dbType))
        .withReturnType(JavaTypes.LIST().getGenericType().getParameterizedType(dbType))
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.findFromTemplate(
                  getOwnersType().getName() + ".find()",
                  getOwnersType(),
                  (CachedDBObject) args[0],
                  null,
                  false,
                  -1,
                  -1);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _findSortedMethod = new MethodInfoBuilder().withName("findSorted").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("template").withType(dbType),
            new ParameterInfoBuilder().withName("sortProperty").withType(TypeSystem.get(PropertyReference.class).getParameterizedType(dbType, JavaTypes.OBJECT())),
            new ParameterInfoBuilder().withName("ascending").withType(JavaTypes.pBOOLEAN()))
        .withReturnType(JavaTypes.LIST().getGenericType().getParameterizedType(dbType))
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.findFromTemplate(
                  getOwnersType().getName() + ".findSorted()",
                  getOwnersType(),
                  (CachedDBObject) args[0],
                  (PropertyReference) args[1],
                  (Boolean) args[2],
                  -1,
                  -1);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _findPagedMethod = new MethodInfoBuilder().withName("findPaged").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("template").withType(dbType),
            new ParameterInfoBuilder().withName("pageSize").withType(JavaTypes.pINT()),
            new ParameterInfoBuilder().withName("offset").withType(JavaTypes.pINT()))
        .withReturnType(JavaTypes.LIST().getGenericType().getParameterizedType(dbType))
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.findFromTemplate(
                  getOwnersType().getName() + ".findPaged()",
                  getOwnersType(),
                  (CachedDBObject) args[0],
                  null,
                  false,
                  (Integer) args[1],
                  (Integer) args[2]);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);
    _findSortedPagedMethod = new MethodInfoBuilder().withName("findSortedPaged").withStatic()
        .withParameters(new ParameterInfoBuilder().withName("template").withType(dbType),
            new ParameterInfoBuilder().withName("sortProperty").withType(TypeSystem.get(PropertyReference.class).getParameterizedType(dbType, JavaTypes.OBJECT())),
            new ParameterInfoBuilder().withName("ascending").withType(JavaTypes.pBOOLEAN()),
            new ParameterInfoBuilder().withName("pageSize").withType(JavaTypes.pINT()),
            new ParameterInfoBuilder().withName("offset").withType(JavaTypes.pINT()))
        .withReturnType(JavaTypes.LIST().getGenericType().getParameterizedType(dbType))
        .withCallHandler(new IMethodCallHandler() {
          @Override
          public Object handleCall(Object ctx, Object... args) {
            try {
              return _queryExecutor.findFromTemplate(
                  getOwnersType().getName() + ".findSortedPaged()",
                  getOwnersType(),
                  (CachedDBObject) args[0],
                  (PropertyReference) args[1],
                  (Boolean) args[2],
                  (Integer) args[3],
                  (Integer) args[4]);
            } catch (SQLException e) {
              throw new RuntimeException(e);
            }
          }
        }).build(this);

    _newProperty = new PropertyInfoBuilder().withName("_New").withType(JavaTypes.pBOOLEAN())
        .withWritable(false).withAccessor(new IPropertyAccessor() {
          @Override
          public void setValue(Object ctx, Object value) {
          }

          @Override
          public Object getValue(Object ctx) {
            return ((CachedDBObject) ctx).isNew();
          }
        }).build(this);
    _properties = new HashMap<String, IPropertyInfo>();
    for (IDBColumn column : dbType.getTable().getColumns()) {
      // TODO - AHK - Ideally this cast wouldn't be necessary
      IPropertyInfo prop = makeProperty((DBColumnImpl) column);
      _properties.put(prop.getName(), prop);
    }

    _ctor = new ConstructorInfoBuilder()
        .withConstructorHandler(new IConstructorHandler() {
          @Override
          public Object newInstance(Object... args) {
            return new CachedDBObject(getOwnersType(), true);
          }
        }).build(this);

    _methods = new ArrayList<IMethodInfo>(Arrays.asList(_getMethod, _idMethod, _updateMethod, _deleteMethod, _countWithSqlMethod,
        _countMethod, _findWithSqlMethod, _findMethod, _findSortedMethod, _findPagedMethod,
        _findSortedPagedMethod));

    CommonServices.getEntityAccess().addEnhancementMethods(dbType, _methods);
    CommonServices.getEntityAccess().addEnhancementProperties(dbType, _properties, true);
  }

  @Override
  public List<? extends IPropertyInfo> getProperties() {
    List<IPropertyInfo> props = new ArrayList<IPropertyInfo>(_properties.values());
    props.addAll(_arrayProperties.get().values());
    props.add(_newProperty);
    return props;
  }

  @Override
  public IPropertyInfo getProperty(CharSequence propName) {
    if (propName.equals("_New")) {
      return _newProperty;
    }
    IPropertyInfo prop = _properties.get(propName.toString());
    if (prop == null) {
      prop = _arrayProperties.get().get(propName.toString());
    }
    return prop;
  }

  @Override
  public CharSequence getRealPropertyName(CharSequence propName) {
    for (String key : _properties.keySet()) {
      if (key.equalsIgnoreCase(propName.toString())) {
        return key;
      }
    }
    for (String key : _arrayProperties.get().keySet()) {
      if (key.equalsIgnoreCase(propName.toString())) {
        return key;
      }
    }
    return propName;
  }

  @Override
  public List<? extends IMethodInfo> getMethods() {
    return _methods;
  }

  @Override
  public IMethodInfo getMethod(CharSequence methodName, IType... params) {
    if ("fromID".equals(methodName) && params != null && params.length == 1 && params[0].equals(JavaTypes.pLONG())) {
      return _getMethod;
    }
    if ("toID".equals(methodName) && (params == null || params.length == 0)) {
      return _idMethod;
    }
    if ("update".equals(methodName) && (params == null || params.length == 0)) {
      return _updateMethod;
    }
    if ("delete".equals(methodName) && (params == null || params.length == 0)) {
      return _deleteMethod;
    }
    if ("findWithSql".equals(methodName) && params != null && params.length == 1 && params[0].equals(JavaTypes.STRING())) {
      return _findWithSqlMethod;
    }
    if ("find".equals(methodName) && params != null && params.length == 1 && params[0].equals(getOwnersType())) {
      return _findMethod;
    }
    if ("findSorted".equals(methodName) && params != null && params.length == 3 && params[0].equals(getOwnersType()) && TypeSystem.get(PropertyReference.class).isAssignableFrom(params[1]) && params[2].equals(JavaTypes.pBOOLEAN())) {
      return _findSortedMethod;
    }
    if ("findPaged".equals(methodName) && params != null && params.length == 3 && params[0].equals(getOwnersType()) && params[1].equals(JavaTypes.pINT()) && params[2].equals(JavaTypes.pINT())) {
      return _findPagedMethod;
    }
    if ("findSortedPaged".equals(methodName) && params != null && params.length == 5 && params[0].equals(getOwnersType()) && TypeSystem.get(PropertyReference.class).isAssignableFrom(params[1]) && params[2].equals(JavaTypes.pBOOLEAN()) && params[3].equals(JavaTypes.pINT()) && params[4].equals(JavaTypes.pINT())) {
      return _findSortedPagedMethod;
    }
    if ("count".equals(methodName) && params != null && params.length == 1 && params[0].equals(getOwnersType())) {
      return _countMethod;
    }
    if ("countWithSql".equals(methodName) && params != null && params.length == 1 && params[0].equals(JavaTypes.STRING())) {
      return _countWithSqlMethod;
    }
    return null;
  }

  @Override
  public IMethodInfo getCallableMethod(CharSequence method, IType... params) {
    return getMethod(method, params);
  }

  @Override
  public List<? extends IConstructorInfo> getConstructors() {
    return Collections.singletonList(_ctor);
  }

  @Override
  public IConstructorInfo getConstructor(IType... params) {
    if (params == null || params.length == 0) {
      return _ctor;
    }
    return null;
  }

  @Override
  public IConstructorInfo getCallableConstructor(IType... params) {
    return getConstructor(params);
  }

  private Map<String, IPropertyInfo> makeArrayProperties() {
    Map<String, IPropertyInfo> arrayProps = new HashMap<String, IPropertyInfo>();
    for (IDBArray dbArray : getOwnersType().getTable().getArrays()) {
      IPropertyInfo arrayProp = makeArrayProperty(dbArray);
      arrayProps.put(arrayProp.getName(), arrayProp);
    }
    return arrayProps;
  }

  private DBPropertyInfo makeProperty(DBColumnImpl column) {
    return new DBPropertyInfo(this, column);
  }

  private IPropertyInfo makeArrayProperty(IDBArray dbArray) {
    return new DBArrayPropertyInfo(this, dbArray);
  }

  @Override
  public IDBType getOwnersType() {
    return (IDBType) super.getOwnersType();
  }


}
