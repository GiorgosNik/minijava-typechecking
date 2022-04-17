package src;

import syntaxtree.*;
import src.classMap;
import src.method;
import src.variable;
import visitor.GJDepthFirst;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Visitor extends GJDepthFirst<String, Object> {

    public Map<String, classMap> classes;

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
        classes = new HashMap<String, classMap>();
        String classname = n.f1.accept(this, null);
        System.out.println("Class: " + classname);

        super.visit(n, argu);

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
        //System.out.println("Class: " + classname);
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
        //System.out.println("Class: " + classname);

        super.visit(n, argu);


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
            //System.out.println(type + " " + varName);
        } else if (argu.getClass() == classMap.class) {
            String type = n.f0.accept(this, null);
            String varName = n.f1.accept(this, null);
            ((classMap) argu).addField(varName, type);
            //System.out.println(type + " " + varName);
        } else {
            String type = n.f0.accept(this, null);
            String varName = n.f1.accept(this, null);
            //System.out.println(type + " " + varName);

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
        ((classMap) argu).addMethod(new method(myName,myType));
        n.f4.accept(this, ((classMap) argu).methods.get(myName));
        //System.out.println(myType + " " + myName);
        //System.out.println("Method Variables");
        n.f7.accept(this, ((classMap) argu).methods.get(myName));

        n.f8.accept(this, null);

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Object argu) throws Exception {
        n.f0.accept(this, argu);

        if (n.f1 != null) {
            n.f1.accept(this, argu);
        }

        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Object argu) throws Exception {

        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Object argu) throws Exception {
        String ret = "";
        for (Node node : n.f0.nodes) {
            ret += ", " + node.accept(this, argu);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Object argu) throws Exception {
        //System.out.println("Parameter");
        if (argu.getClass() == method.class) {
            String type = n.f0.accept(this, null);
            String name = n.f1.accept(this, null);
            ((method) argu).addFormalParam(name, type);
        } else{
            throw new Exception("Bad Argu Type");
        }
        
        return null;
    }

    @Override
    public String visit(ArrayType n, Object argu) {
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