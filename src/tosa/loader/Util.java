package tosa.loader;

import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaType;

import java.sql.Types;

/**
 * Created by IntelliJ IDEA.
 * User: alan
 * Date: 12/29/10
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {
  static IType getJavaType(int sqlType) {
    switch (sqlType) {
      case Types.BIT:
        return IJavaType.BOOLEAN;

      case Types.TINYINT:
        return IJavaType.BYTE;

      case Types.SMALLINT:
        return IJavaType.SHORT;

      case Types.INTEGER:
        return IJavaType.INTEGER;

      case Types.BIGINT:
        return IJavaType.LONG;

      case Types.FLOAT:
        return IJavaType.DOUBLE;

      case Types.REAL:
        return IJavaType.FLOAT;

      case Types.DOUBLE:
        return IJavaType.DOUBLE;

      case Types.NUMERIC:
        return IJavaType.BIGDECIMAL;

      case Types.DECIMAL:
        return IJavaType.BIGDECIMAL;

      case Types.CHAR:
        return IJavaType.STRING;

      case Types.VARCHAR:
        return IJavaType.STRING;

      case Types.LONGVARCHAR:
        return IJavaType.STRING;

      case Types.BOOLEAN:
        return IJavaType.BOOLEAN;

      case Types.DATE:
        return TypeSystem.get(java.sql.Date.class);

      case Types.TIME:
        return TypeSystem.get(java.sql.Time.class);

      case Types.TIMESTAMP:
        return TypeSystem.get(java.sql.Timestamp.class);

      case Types.BINARY:
        return IJavaType.pBYTE.getArrayType();

      case Types.VARBINARY:
        return IJavaType.pBYTE.getArrayType();

      case Types.LONGVARBINARY:
        return IJavaType.pBYTE.getArrayType();

      case Types.NULL:
        return IJavaType.pVOID;

      case Types.OTHER:
        return IJavaType.OBJECT;

      case Types.JAVA_OBJECT:
        return IJavaType.OBJECT;

      case Types.DISTINCT:
        return IJavaType.OBJECT;

      case Types.STRUCT:
        return IJavaType.OBJECT;

      case Types.ARRAY:
        return IJavaType.OBJECT.getArrayType();

      case Types.BLOB:
        return IJavaType.OBJECT;

      case Types.CLOB:
        return IJavaType.STRING;

      case Types.REF:
        return IJavaType.OBJECT;

      case Types.DATALINK:
        return IJavaType.OBJECT;

    }
    return IJavaType.OBJECT;
  }

}
