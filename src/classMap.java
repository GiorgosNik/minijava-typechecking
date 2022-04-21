package src;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class classMap {
    String Name;
    public Map<String, method> methods;
    public Map<String, variable> fields;
    int[] fieldOffset;
    int[] methodOffset;
    List<String> fieldOffsets;
    List<String> methodOffsets;

    classMap parentClass;

    public classMap(String name) {
        Name = name;
        parentClass = null;
        fieldOffsets  = new ArrayList<>();
        methodOffsets  = new ArrayList<>();
        methods = new LinkedHashMap<String, method>();
        fields = new LinkedHashMap<String, variable>();
    }
    public classMap(String name, classMap parent) {
        Name = name;
        parentClass = parent;
        fieldOffsets  = new ArrayList<>();
        methodOffsets  = new ArrayList<>();
        methods = new LinkedHashMap<String, method>();
        fields = new LinkedHashMap<String, variable>();
    }

    public void addField(String name, String type)throws Exception{
        if (fields.containsKey(name)) {
            throw new Exception("Field Exists");
         } else {
            fields.put(name, new variable(name,type));
         }
    }
    public void addMethod(method newMethod)throws Exception{
        if(methods.containsKey(newMethod.Name)){
            throw new Exception("Method Exists");
        }else{
            methods.put(newMethod.Name, newMethod);
        }
    }
    public void print(){
        System.out.println();
        if(parentClass != null){
            System.out.println("-----------Class: "+Name+" extends: "+parentClass.Name+"-----------");
        }else{
            System.out.println("-----------Class: "+Name+"-----------");
        }
        System.out.println("--Variables---");
        for(int i = 0;i<fieldOffsets.size();i++){
            System.out.println(fieldOffsets.get(i));
        }
        System.out.println();
        System.out.println("---Methods---");
        for(int i = 0;i<methodOffsets.size();i++){
            System.out.println(methodOffsets.get(i));
        }
        System.out.println();
    }
}