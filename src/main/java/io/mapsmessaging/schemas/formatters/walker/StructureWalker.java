package io.mapsmessaging.schemas.formatters.walker;

import io.mapsmessaging.selector.IdentifierResolver;
import java.math.BigDecimal;
import java.util.List;

public class StructureWalker {


  public static Object locateObject(IdentifierResolver resolver, List<String> searchPath){
    Object context = null;
    while(!searchPath.isEmpty()){
      var path = searchPath.remove(0);
      context = resolver.get(path);
      if(context instanceof  IdentifierResolver){
        resolver= (IdentifierResolver) context;
      }
    }
    return parse(context);
  }

  private static Object parse(Object lookup){
    if(lookup == null) return null;
    if (lookup instanceof String ||
        lookup instanceof Float ||
        lookup instanceof Double ||
        lookup instanceof Byte ||
        lookup instanceof Character ||
        lookup instanceof Short ||
        lookup instanceof Integer ||
        lookup instanceof Long) {
      return lookup;
    }
    else if(lookup instanceof BigDecimal){
      return ((BigDecimal)lookup).doubleValue();
    }
    return lookup.toString();
  }
}
