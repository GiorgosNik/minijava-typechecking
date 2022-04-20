package src;

import syntaxtree.*;
import src.classMap;
import src.method;
import src.variable;
import visitor.GJDepthFirst;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
public class VisitorPhase1 extends GJDepthFirst<String, Object> {

    public LinkedHashMap<String, classMap> classes;

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "String"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Object argu) throws Exception {
        classes = new LinkedHashMap<String, classMap>();
        String classname = n.f1.accept(this, null);
        if (classes.containsKey(classname)) {
            throw new Exception("Class Exists");
        } else {
            classes.put(classname, new classMap(classname));
        }
        classes.get(classname).addMethod(new method("main", "void", classes.get(classname)));
        classes.get(classname).methods.get("main").addFormalParam(n.f11.accept(this, null), "String[]");
        n.f14.accept(this, classes.get(classname).methods.get("main"));
        classes.get(classname).print();
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Object argu) throws Exception {
        String classname = n.f1.accept(this, null);
        if (classes.containsKey(classname)) {
            throw new Exception("Class Exists");
        } else {
            classes.put(classname, new classMap(classname));
        }
        n.f3.accept(this, (Object) classes.get(classname));
        n.f4.accept(this, (Object) classes.get(classname));
        classes.get(classname).print();
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Object argu) throws Exception {
        String classname = n.f1.accept(this, null);
        String parent = n.f3.accept(this, null);
        if (classes.containsKey(classname)) {
            throw new Exception("Class Exists");
        } else if (classes.containsKey(parent)) {
            classes.put(classname, new classMap(classname, classes.get(parent)));
        } else {
            throw new Exception("Parent class does not exist");
        }
        n.f5.accept(this, (Object) classes.get(classname));
        n.f6.accept(this, (Object) classes.get(classname));
        classes.get(classname).print();
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Object argu) throws Exception {
        if (argu.getClass() == method.class) {
            String type = n.f0.accept(this, null);
            String varName = n.f1.accept(this, null);
            ((method) argu).addDefinedVar(varName, type);
        } else if (argu.getClass() == classMap.class) {
            String type = n.f0.accept(this, null);
            String varName = n.f1.accept(this, null);
            ((classMap) argu).addField(varName, type);
        }
        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Object argu) throws Exception {
        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        ((classMap) argu).addMethod(new method(myName, myType, (classMap) argu));
        n.f4.accept(this, ((classMap) argu).methods.get(myName));
        if(((classMap) argu).parentClass!= null){
            if(((classMap) argu).parentClass.methods.containsKey(myName)){
                method parentMethod = ((classMap) argu).parentClass.methods.get(myName);
                LinkedHashMap<String, variable> parentParams = parentMethod.formalParams;
                List<variable> parentArgumentTypes= new ArrayList<variable>(parentParams.values());
                List<variable> myArgumentTypes= new ArrayList<variable>(((classMap) argu).methods.get(myName).formalParams.values());
                if(myArgumentTypes.size() != parentArgumentTypes.size()){
                    throw new Exception("Exception: Arguments do not match overwriting method");
                }else{
                    for (int i = 0; i < parentArgumentTypes.size(); i++) {
                        if(!parentArgumentTypes.get(i).Type.equals(myArgumentTypes.get(i).Type)){
                            throw new Exception("Exception: Arguments do not match overwriting method");
                        }
                    }
                }
            }
        }
        n.f7.accept(this, ((classMap) argu).methods.get(myName));
        n.f8.accept(this, null);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Object argu) throws Exception {
        if (argu.getClass() == method.class) {
            String type = n.f0.accept(this, null);
            String name = n.f1.accept(this, null);
            ((method) argu).addFormalParam(name, type);
        } else {
            throw new Exception("Bad Argu Type");
        }

        return null;
    }

    @Override
    public String visit(BooleanArrayType n, Object argu) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, Object argu) {
        return "int[]";
    }

    public String visit(BooleanType n, Object argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Object argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Object argu) {
        return n.f0.toString();
    }
}