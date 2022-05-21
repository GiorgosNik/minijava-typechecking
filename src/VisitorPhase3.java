package src;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

public class VisitorPhase3 extends GJDepthFirst<String, Object> {
    public LinkedHashMap<String, classMap> classes;
    public LinkedHashMap<String, String> variableRegister;
    public int registerCounter;
    public int labelCounter;

    public String loadVarToRegister(String name, String Type) {
        if (Type == "int[]") {
            System.out.println("%_" + registerCounter + " = load i32*, i32** " + variableRegister.get(name));
        } else if (Type == "int") {
            System.out.println("%_" + registerCounter + " = load i32, i32* " + variableRegister.get(name));
        } else if (Type == "boolean") {
            System.out.println("%_" + registerCounter + " = load i1, i1* " + variableRegister.get(name));
        } else if (classes.containsKey(Type)) {
            System.out.println("%_" + registerCounter + " = load i8*, i8** " + variableRegister.get(name));
        }
        String result = " %_" + registerCounter;
        registerCounter++;
        return result;
    }

    public String isVar(String name, method currMethod) throws Exception {
        // Use this function, to retrieve a variable's or Object's type, given its name
        // and scope
        String type = null;
        if ((name.equals("int") || name.equals("boolean") || name.equals("int[]") || name.equals("boolean[]")
                || classes.containsKey(name))) {
            return name;
        }
        if (currMethod.formalParams.containsKey(name)) {
            type = currMethod.formalParams.get(name).Type;
        } else if (currMethod.definedVars.containsKey(name)) {
            type = currMethod.definedVars.get(name).Type;
        } else if (currMethod.belongsTo.fields.containsKey(name)) {
            type = currMethod.belongsTo.fields.get(name).Type;
        } else if (currMethod.belongsTo.parentClass != null) {
            classMap currClass = currMethod.belongsTo;
            while (currClass.parentClass != null) {
                if (currClass.parentClass.fields.containsKey(name)) {
                    type = currClass.parentClass.fields.get(name).Type;
                    break;
                } else {
                    currClass = currClass.parentClass;
                }
            }

        } else {
            return null;
        }
        return type;
    }

    public void passSymbolTable(LinkedHashMap<String, classMap> givenMap) {
        // Used to get the Symbol Table from Visitor #1
        classes = givenMap;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
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

    public String visit(MainClass n, Object argu) throws Exception {
        classMap thisClass = classes.get(n.f1.accept(this, argu));
        List<classMap> parentList = new ArrayList<classMap>();
        LinkedHashMap<String, method> uniqueMethods;
        classMap parentClass = null;
        String methodSubString;
        String methodString;

        // Initialize the counters for the registers and labels;
        registerCounter = 0;
        labelCounter = 0;
        variableRegister = new LinkedHashMap<String, String>();
        System.out.println("\n------------------LLVM TEST------------------\n");

        // Loop for each class
        for (String key : classes.keySet()) {

            // Create the map to hold the inherited methods
            uniqueMethods = new LinkedHashMap<String, method>();

            // If class in not main
            if (key != thisClass.Name) {
                methodString = "";

                // Methods defined in parent classes
                if (classes.get(key).parentClass != null) {
                    parentClass = classes.get(key).parentClass;
                }
                while (parentClass != null) {
                    parentList.add(parentClass);
                    if (parentClass.parentClass != null) {
                        parentClass = parentClass.parentClass;
                    } else {
                        parentClass = null;
                    }
                }

                // Loop for parent classes
                for (int i = 0; i < parentList.size(); i++) {

                    // Loop for methods of parent class
                    for (String parentMethod : parentList.get(i).methods.keySet()) {
                        if (uniqueMethods.containsKey(parentMethod)) {
                            uniqueMethods.replace(parentMethod, parentList.get(i).methods.get(parentMethod));
                        } else {
                            uniqueMethods.put(parentMethod, parentList.get(i).methods.get(parentMethod));
                        }
                    }
                }

                for (String methodKey : classes.get(key).methods.keySet()) {
                    if (uniqueMethods.containsKey(methodKey)) {
                        uniqueMethods.replace(methodKey, classes.get(key).methods.get(methodKey));
                    } else {
                        uniqueMethods.put(methodKey, classes.get(key).methods.get(methodKey));
                    }
                }
                for (String methodKey : uniqueMethods.keySet()) {
                    methodSubString = "i8* bitcast (";
                    if (uniqueMethods.get(methodKey).Type == "int") {
                        methodSubString = methodSubString + "i32";
                    } else if (uniqueMethods.get(methodKey).Type == "boolean") {
                        methodSubString = methodSubString + "i1";
                    } else {
                        methodSubString = methodSubString + "i8*";
                    }
                    methodSubString = methodSubString + "(i8*";
                    for (String argumentKey : uniqueMethods.get(methodKey).formalParams.keySet()) {
                        if (uniqueMethods.get(methodKey).formalParams.get(argumentKey).Type == "int") {
                            methodSubString = methodSubString + ",i32";
                        } else if (uniqueMethods.get(methodKey).formalParams.get(argumentKey).Type == "boolean") {
                            methodSubString = methodSubString + ",i1";
                        } else {
                            methodSubString = methodSubString + ",i8*";
                        }
                    }
                    methodSubString = methodSubString + ")* " + uniqueMethods.get(methodKey).belongsTo.Name + "."
                            + methodKey + " to i8*)";
                    if (methodString != "") {
                        methodString = methodString + ", " + methodSubString;
                    } else {
                        methodString = methodSubString;
                    }
                }
                System.out.println("@." + key + "_vtable = global ["
                        + String.valueOf(uniqueMethods.size()) + " x i8*][" + methodString + "]");

            }
        }
        System.out.println("@." + thisClass.Name + "_vtable = global [0 x i8*] []");
        System.out.println("declare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)");
        System.out.println(
                "\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"");
        System.out.println(
                "\n@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"");
        System.out.println(
                "define void @print_int(i32 %i) {\n %_str = bitcast [4 x i8]* @_cint to i8*\n call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n ret void\n}\n");
        System.out.println(
                "define void @throw_oob() {\n%_str = bitcast [15 x i8]* @_cOOB to i8*\ncall i32 (i8*, ...) @printf(i8* %_str)\ncall void @exit(i32 1)\nret void\n}\n");
        System.out.println(
                "define void @throw_nsz() {\n%_str = bitcast [15 x i8]* @_cNSZ to i8*\ncall i32 (i8*, ...) @printf(i8* %_str)\ncall void @exit(i32 1)\nret void\n}\n");
        System.out.println("define i32 @main() {");

        // Created all V-tables, now start working on the main
        n.f14.accept(this, thisClass.methods.get("main"));
        n.f15.accept(this, thisClass.methods.get("main"));
        System.out.println("ret i32 0");
        System.out.println("}");
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
    public String visit(ClassDeclaration n, Object argu) throws Exception {
        String name = n.f1.accept(this, argu);
        classMap thisClass = classes.get(name);
        n.f4.accept(this, thisClass);
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
    public String visit(ClassExtendsDeclaration n, Object argu) throws Exception {
        String name = n.f1.accept(this, argu);
        classMap thisClass = classes.get(name);
        n.f6.accept(this, thisClass);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Object argu) throws Exception {
        method thisMethod = (method) argu;
        String varName = n.f1.accept(this, null);
        String type = thisMethod.definedVars.get(varName).Type;
        String alloca = "%" + varName + " = alloca";

        if (type == "int") {
            alloca = alloca + " i32";
        } else if (type == "boolean") {
            alloca = alloca + " i1";
        } else if (type == "boolean[]") {
            alloca = alloca + " i1*";
        } else if (type == "int[]") {
            alloca = alloca + " i32*";
        } else {
            alloca = alloca + " i8*";
        }
        variableRegister.put(varName, "%" + varName);
        System.out.println(alloca);

        return null;
    }

    // /**
    // * f0 -> "public"
    // * f1 -> Type()
    // * f2 -> Identifier()
    // * f3 -> "("
    // * f4 -> ( FormalParameterList() )?
    // * f5 -> ")"
    // * f6 -> "{"
    // * f7 -> ( VarDeclaration() )*
    // * f8 -> ( Statement() )*
    // * f9 -> "return"
    // * f10 -> Expression()
    // * f11 -> ";"
    // * f12 -> "}"
    // */
    // public String visit(MethodDeclaration n, Object argu) throws Exception {
    // classMap thisClass = (classMap) argu;
    // String type = n.f1.accept(this, argu);
    // String name = n.f2.accept(this, argu);
    // method thisMethod = thisClass.methods.get(name);
    // String returnType = n.f10.accept(this, thisMethod);
    // // Check method returns a known type
    // if (!(returnType.equals("boolean") || returnType.equals("int") ||
    // returnType.equals("boolean[]")
    // || returnType.equals("int[]"))) {
    // returnType = isVal(returnType, thisMethod);
    // }
    // // Check the returned value actually is of the same type
    // if (!type.equals(returnType)) {
    // throw new Exception("Exception: Return Type does not match returned value");
    // }

    // n.f8.accept(this, thisMethod);
    // return null;
    // }

    // /**
    // * f0 -> "this"
    // */
    // public String visit(ThisExpression n, Object argu) throws Exception {
    // // Get the type of the item referred to by 'this'
    // if (argu.getClass() == method.class) {
    // return ((method) argu).belongsTo.Name;
    // } else if (argu.getClass() == classMap.class) {
    // return ((classMap) argu).Name;
    // } else {
    // throw new Exception("Exception: Bad use of 'THIS'");
    // }
    // }

    // /**
    // * f0 -> Identifier()
    // * f1 -> "="
    // * f2 -> Expression()
    // * f3 -> ";"
    // */
    // @Override
    // public String visit(AssignmentStatement n, Object argu) throws Exception {
    // String name = n.f0.accept(this, argu);
    // String type = isVal(name, (method) argu);
    // String exprType = n.f2.accept(this, argu);
    // // Check the expression is of a known type
    // if (!(exprType.equals("int") || exprType.equals("boolean") ||
    // exprType.equals("int[]")
    // || exprType.equals("boolean[]")) && !classes.containsKey(exprType)) {
    // exprType = isVal(exprType, (method) argu);
    // }

    // // Check the variable is of the same type, or of the parent class if
    // applicable
    // if (!exprType.equals(type)) {
    // if (classes.containsKey(exprType)) {
    // if (classes.get(exprType).parentClass != null) {
    // if (!classes.get(exprType).parentClass.Name.equals(type)) {
    // throw new Exception("Exception: Incomatible Assignement");
    // }
    // } else {
    // throw new Exception("Exception: Incomatible Assignement");
    // }
    // } else {
    // throw new Exception("Exception: Incomatible Assignement");
    // }
    // }
    // return null;
    // }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Object argu) throws Exception {
        String arrayName = n.f0.accept(this, argu);
        String arrayType = isVar(arrayName, (method) argu);
        arrayType = isVar(arrayType, (method) argu);

        // Get the index and calculate the expression
        String arrayIndex = n.f2.accept(this, argu);
        String exprReturn = n.f5.accept(this, argu);

        if (arrayType == "int[]") {
            System.out.println("%_" + registerCounter + " = load i32*, i32** " + variableRegister.get(arrayName));
            registerCounter++;
            System.out.println("%_" + registerCounter + " = load i32, i32* %_" + (registerCounter - 1));
            registerCounter++;
            System.out.println("%_" + registerCounter + " = icmp sge i32 0, " + arrayIndex);
            registerCounter++;
            System.out.println("%_" + registerCounter + " = icmp slt i32 0, %_" + (registerCounter - 2));
            registerCounter++;
            System.out.println(
                    "%_" + registerCounter + " = and i1 %_" + (registerCounter - 2) + ", %_" + (registerCounter - 1));
            registerCounter++;
            System.out.println("br i1 %_8, label %oob_ok_" + labelCounter + ", label %oob_err_" + labelCounter);
            System.out.println("oob_err_" + labelCounter + ":");
            System.out.println("call void @throw_oob()");
            System.out.println("br label %oob_ok_" + labelCounter);
            System.out.println("oob_ok_" + labelCounter + ":");
            System.out.println("%_" + registerCounter + " = add i32 1, " + arrayIndex);
            registerCounter++;
            System.out.println("%_" + registerCounter + " = getelementptr i32, i32* %_" + (registerCounter - 6)
                    + ", i32 %_" + (registerCounter - 1));
            registerCounter++;
            System.out.println("store i32 " + exprReturn + ", i32* %_" + (registerCounter - 1));//
            labelCounter++;
        } else {
            // To do, use this https://piazza.com/class/kzyd8bi9p0ihq?cid=86 ?
        }

        return null;
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, Object argu) throws Exception {
        String ifCondtion = n.f2.accept(this, argu);
        int ifLabel = labelCounter;

        if (isVar(ifCondtion, (method) argu) != null) {
            ifCondtion = loadVarToRegister(ifCondtion, "boolean");
        }

        labelCounter++;
        System.out.println("br i1 " + ifCondtion + ", label %if_then_" + ifLabel + ", label %if_else_" + ifLabel);

        System.out.println("if_then_" + ifLabel + ":");
        n.f4.accept(this, argu);
        System.out.println("br label %if_end_" + ifLabel);

        System.out.println("if_else_" + ifLabel + ":");
        n.f6.accept(this, argu);
        System.out.println("br label %if_end_" + ifLabel);

        System.out.println("if_end_" + ifLabel + ":");
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Object argu) throws Exception {
        int WhileExpressionLabel = labelCounter;
        labelCounter++;
        
        // Jump to the start of the loop
        System.out.println("br label %while_top_"+WhileExpressionLabel);
        System.out.println("while_top_" +WhileExpressionLabel+ ":");

        // Create condition and jump based on result
        String loopCondtion = n.f2.accept(this, argu);
        if (isVar(loopCondtion, (method) argu) != null) {
            loopCondtion = loadVarToRegister(loopCondtion, "boolean");
        }
        System.out.println("br i1 " + loopCondtion + ", label %while_in_" + WhileExpressionLabel + ", label %while_out_" + WhileExpressionLabel);

        // In the loop
        System.out.println("while_in_" +WhileExpressionLabel+ ":");
        n.f4.accept(this, argu);
        System.out.println("br label %while_top_"+WhileExpressionLabel);

        System.out.println("while_out_" +WhileExpressionLabel+ ":");
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Object argu) throws Exception {
        String varName = n.f2.accept(this, argu);
        method thisMethod = (method) argu;
        if (isVar(varName, thisMethod) != null) {
            varName = loadVarToRegister(varName, isVar(varName, thisMethod));
        }
        System.out.println("call void (i32) @print_int(i32 " + varName + ")");
        registerCounter++;
        return null;
    }

    // /**
    // * f0 -> PrimaryExpression()
    // * f1 -> "."
    // * f2 -> Identifier()
    // * f3 -> "("
    // * f4 -> ( ExpressionList() )?
    // * f5 -> ")"
    // */
    // @Override
    // public String visit(MessageSend n, Object argu) throws Exception {
    // String className = n.f0.accept(this, argu);
    // method toCall = null;

    // // Check that the object or class the method is called on, actually exists
    // if ((!classes.containsKey(className)) && (!(((method)
    // argu).formalParams.containsKey(className)
    // || ((method) argu).definedVars.containsKey(className)))) {
    // if (!((method) argu).belongsTo.fields.containsKey(className)) {
    // if (((method) argu).belongsTo.parentClass != null) {
    // if (!((method) argu).belongsTo.parentClass.fields.containsKey(className)) {
    // throw new Exception("Exception: No such class or variable");
    // }
    // } else {
    // throw new Exception("Exception: No such class or variable");
    // }
    // }
    // }

    // // If the method is inherited, set the className to be the one of the parent
    // // class for future reference
    // if (argu != null && !classes.containsKey(className)) {
    // if (((method) argu).formalParams.containsKey(className)) {
    // className = ((method) argu).formalParams.get(className).Type;
    // } else if (((method) argu).definedVars.containsKey(className)) {
    // className = ((method) argu).definedVars.get(className).Type;
    // } else if (((method) argu).belongsTo.fields.containsKey(className)) {
    // className = ((method) argu).belongsTo.fields.get(className).Type;
    // } else if (((method)
    // argu).belongsTo.parentClass.fields.containsKey(className)) {
    // className = ((method) argu).belongsTo.parentClass.fields.get(className).Type;
    // }
    // }

    // // Check the method belongs to the class
    // String funcName = n.f2.accept(this, argu);
    // if (classes.get(className).methods.containsKey(funcName)) {
    // toCall = classes.get(className).methods.get(funcName);
    // } else if (classes.get(className).parentClass != null) {
    // while (classes.get(className).parentClass != null) {
    // if (classes.get(className).parentClass.methods.containsKey(funcName)) {
    // toCall = classes.get(className).parentClass.methods.get(funcName);
    // break;
    // } else {
    // className = classes.get(className).parentClass.Name;
    // }
    // }
    // if (toCall == null) {
    // throw new Exception("Exception: No such method");
    // }

    // } else {
    // System.out.println(className + " " + funcName);
    // throw new Exception("Exception: No such method");
    // }

    // // Check that the arguments and the formal parameters match
    // // First get both, and compare their size
    // String typeString = "" + n.f4.accept(this, argu);
    // if (typeString.length() == 0 && !toCall.formalParams.isEmpty()) {
    // throw new Exception("Exception: Arguments do not match");
    // }
    // String[] parts = typeString.split(",");
    // if (parts.length != toCall.formalParams.size() &&
    // !toCall.formalParams.isEmpty()) {
    // throw new Exception("Exception: Arguments do not match");
    // }

    // // If they are the same size, check the type of each argument in order
    // List<variable> formalParams = new
    // ArrayList<variable>(toCall.formalParams.values());
    // for (int i = 0; i < formalParams.size(); i++) {
    // if (!(formalParams.get(i).Type.equals(isVal(parts[i], ((method) argu))))) {
    // if (classes.containsKey(formalParams.get(i).Type)) {
    // if (classes.containsKey(isVal(parts[i], ((method) argu)))) {
    // if (!classes.get(isVal(parts[i], ((method) argu))).parentClass.Name
    // .equals(formalParams.get(i).Type)) {
    // System.out.println(classes.get(isVal(parts[i], ((method)
    // argu))).parentClass.Name + " "
    // + formalParams.get(i).Type);
    // if (!(classes.get(isVal(parts[i], ((method)
    // argu))).parentClass.parentClass.Name
    // .equals(formalParams.get(i).Type))) {
    // throw new Exception("Exception: Arguments do not match");
    // }
    // }
    // } else {
    // throw new Exception("Exception: Arguments do not match");
    // }
    // } else {
    // throw new Exception("Exception: Arguments do not match");
    // }
    // }
    // }
    // return toCall.Type;
    // }

    // /**
    // * f0 -> Expression()
    // * f1 -> ExpressionTail()
    // */
    // @Override
    // public String visit(ExpressionList n, Object argu) throws Exception {
    // String typeString = n.f0.accept(this, argu);
    // if (n.f1 != null) {
    // typeString += n.f1.accept(this, argu);
    // }
    // return typeString;
    // }

    // /**
    // * f0 -> ( ExpressionTerm() )*
    // */
    // @Override
    // public String visit(ExpressionTail n, Object argu) throws Exception {
    // String ret = "";
    // for (Node node : n.f0.nodes) {
    // ret += "," + node.accept(this, argu);
    // }
    // return ret;
    // }

    // /**
    // * f0 -> ","
    // * f1 -> Expression()
    // */
    // @Override
    // public String visit(ExpressionTerm n, Object argu) throws Exception {
    // return n.f1.accept(this, argu);
    // }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, Object argu) throws Exception {
        String clause1 = n.f0.accept(this, argu);
        String secondClause = null;
        int AndExpressionLabel = labelCounter;
        labelCounter++;
        if (isVar(clause1, (method) argu) != null) {
            clause1 = loadVarToRegister(clause1, "boolean");
        }
        System.out.println("br i1 " + clause1 + ", label %exp_res_1_" + AndExpressionLabel + ", label %exp_res_0_"
                + AndExpressionLabel);

        // exp_res_1
        System.out.println("exp_res_0_" + AndExpressionLabel + ":");
        System.out.println("br label %exp_res_3_" + AndExpressionLabel);

        // exp_res_1
        System.out.println("exp_res_1_" + AndExpressionLabel + ":");
        String clause2 = n.f2.accept(this, argu);
        if (isVar(clause2, (method) argu) != null) {
            clause2 = loadVarToRegister(clause2, "boolean");
        }
        secondClause = clause2;
        System.out.println("br label %exp_res_2_" + AndExpressionLabel);

        // exp_res_2
        System.out.println("exp_res_2_" + AndExpressionLabel + ":");
        System.out.println("br label %exp_res_3_" + AndExpressionLabel);

        // exp_res_3
        System.out.println("exp_res_3_" + AndExpressionLabel + ":");
        System.out.println("%_" + registerCounter + " = phi i1  [ 0, %exp_res_0_" + AndExpressionLabel + " ], [ "
                + secondClause + ", %exp_res_2_" + AndExpressionLabel + " ]");
        registerCounter++;
        return "%_" + (registerCounter - 1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, Object argu) throws Exception {
        String name1 = n.f0.accept(this, argu);
        String type1 = isVar(name1, (method) argu);
        String name2 = n.f2.accept(this, argu);
        String type2 = isVar(name2, (method) argu);
        String register1;
        String register2;

        if (type1 != null) {
            register1 = loadVarToRegister(name1, type1);
        } else {
            register1 = name1;
        }
        if (type2 != null) {
            register2 = loadVarToRegister(name2, type2);
        } else {
            register2 = name2;
        }
        System.out.println("%_" + registerCounter + " = icmp slt i32 " + register1 + ", " + register2);
        registerCounter++;
        return "%_" + (registerCounter - 1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, Object argu) throws Exception {
        String name1 = n.f0.accept(this, argu);
        String type1 = isVar(name1, (method) argu);
        String name2 = n.f2.accept(this, argu);
        String type2 = isVar(name2, (method) argu);
        String register1;
        String register2;

        if (type1 != null) {
            register1 = loadVarToRegister(name1, type1);
        } else {
            register1 = name1;
        }
        if (type2 != null) {
            register2 = loadVarToRegister(name2, type2);
        } else {
            register2 = name2;
        }
        System.out.println("%_" + registerCounter + " = add i32 " + register1 + ", " + register2);
        registerCounter++;
        return "%_" + (registerCounter - 1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, Object argu) throws Exception {
        String name1 = n.f0.accept(this, argu);
        String type1 = isVar(name1, (method) argu);
        String name2 = n.f2.accept(this, argu);
        String type2 = isVar(name2, (method) argu);
        String register1;
        String register2;

        if (type1 != null) {
            register1 = loadVarToRegister(name1, type1);
        } else {
            register1 = name1;
        }
        if (type2 != null) {
            register2 = loadVarToRegister(name2, type2);
        } else {
            register2 = name2;
        }
        System.out.println("%_" + registerCounter + " = sub i32 " + register1 + ", " + register2);
        registerCounter++;
        return "%_" + (registerCounter - 1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    @Override
    public String visit(ArrayLookup n, Object argu) throws Exception {
        String arrayName = n.f0.accept(this, argu);
        String arrayType = isVar(arrayName, (method) argu);
        String indexName = n.f2.accept(this, argu);
        String indexType = isVar(indexName, (method) argu);
        String arrayRegister;
        String indexRegister;
        if (arrayType != null) {
            arrayRegister = loadVarToRegister(arrayName, arrayType);
        } else {
            arrayRegister = arrayName;
        }
        if (indexType != null) {
            indexRegister = loadVarToRegister(indexName, indexType);
        } else {
            indexRegister = indexName;
        }

        if (arrayType.equals("int[]")) {
            System.out.println("%_" + registerCounter + " = load i32, i32* " + arrayRegister);
            registerCounter++;
            System.out.println("%_" + registerCounter + " = icmp sge i32 " + indexRegister + ", 0");
            registerCounter++;
            System.out.println(
                    "%_" + registerCounter + " = icmp slt i32 " + indexRegister + ", %_" + (registerCounter - 2));
            registerCounter++;
            System.out.println(
                    "%_" + registerCounter + " = and i1 %_" + (registerCounter - 2) + ", %_" + (registerCounter - 1));
            registerCounter++;
            System.out.println("br i1 %_" + (registerCounter - 1) + ", label %oob_ok_" + labelCounter
                    + ", label %oob_err_" + labelCounter);

            System.out.println("oob_err_" + labelCounter + ":");
            System.out.println("call void @throw_oob()");
            System.out.println("br label %oob_ok_" + labelCounter);
            System.out.println("oob_ok_" + labelCounter + ":");
            System.out.println("%_" + registerCounter + " = add i32 1, " + indexRegister);
            registerCounter++;
            System.out.println("%_" + registerCounter + " = getelementptr i32, i32* %_" + (registerCounter - 6)
                    + " , i32 %_" + (registerCounter - 1));
            registerCounter++;
            System.out.println("%_" + registerCounter + " = load i32, i32* %_" + (registerCounter - 1));
            registerCounter++;
            labelCounter++;
        }
        return "%_" + (registerCounter - 1);
    }

    // /**
    // * f0 -> NotExpression()
    // * | PrimaryExpression()
    // */
    // @Override
    // public String visit(Clause n, Object argu) throws Exception {
    // String type = n.f0.accept(this, argu);
    // return type;
    // }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, Object argu) throws Exception {
        String name1 = n.f0.accept(this, argu);
        String type1 = isVar(name1, (method) argu);
        String name2 = n.f2.accept(this, argu);
        String type2 = isVar(name2, (method) argu);
        String register1;
        String register2;

        if (type1 != null) {
            register1 = loadVarToRegister(name1, type1);
        } else {
            register1 = name1;
        }
        if (type2 != null) {
            register2 = loadVarToRegister(name2, type2);
        } else {
            register2 = name2;
        }
        System.out.println("%_" + registerCounter + " = mul i32 " + register1 + ", " + register2);
        registerCounter++;
        return "%_" + (registerCounter - 1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Object argu) throws Exception {
        String arrayName = n.f0.accept(this, argu);
        String arrayType = isVar(arrayName, (method) argu);
        String arrayRegister;
        if (arrayType != null) {
            arrayRegister = loadVarToRegister(arrayName, arrayType);
        } else {
            arrayRegister = arrayName;
        }
        if (arrayType.equals("int[]")) {
            System.out.println("%_" + registerCounter + " = load i32, i32* " + arrayRegister);
            registerCounter++;
            labelCounter++;
        }
        return "%_" + (registerCounter - 1);
    }

    // /**
    // * f0 -> IntegerLiteral()
    // * | TrueLiteral()
    // * | FalseLiteral()
    // * | Identifier()
    // * | ThisExpression()
    // * | ArrayAllocationExpression()
    // * | AllocationExpression()
    // * | BracketExpression()
    // */
    // @Override
    // public String visit(PrimaryExpression n, Object argu) throws Exception {
    // String type = n.f0.accept(this, argu);
    // return type;
    // }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, Object argu) throws Exception {
        method thisMethod = (method) argu;
        String identifier = n.f0.accept(this, argu);
        String register = variableRegister.get(identifier);
        String type = thisMethod.definedVars.get(identifier).Type;
        // Check for class fields here
        String expressionResult = n.f2.accept(this, argu);

        // 2. Check type of identifier
        if (type == "int") {
            if (thisMethod.definedVars.containsKey(expressionResult)
                    || thisMethod.formalParams.containsKey(expressionResult)) {
                System.out.println(
                        "%_" + registerCounter + " = load i32, i32* " + variableRegister.get(expressionResult));
                registerCounter++;
            }
            // If the expresion is an integer literal
            if (expressionResult.chars().allMatch(Character::isDigit)) {
                System.out.println("store i32 " + expressionResult + ", i32* " + register);
            } else {
                // If the expresion is an integer variable
                System.out.println("store i32 %_" + (registerCounter - 1) + ", i32* " + register);
            }
        } else if (type == "boolean") {
            if (thisMethod.definedVars.containsKey(expressionResult)
                    || thisMethod.formalParams.containsKey(expressionResult)) {
                System.out
                        .println("%_" + registerCounter + " = load i1, i1* " + variableRegister.get(expressionResult));
                registerCounter++;
            }
            if (expressionResult == "true") {
                System.out.println("store i1 1, i1* " + register);
            } else if (expressionResult == "false") {
                System.out.println("store i1 0, i1* " + register);
            } else {
                // If the expresion is an boolean variable
                System.out.println("store i1 %_" + (registerCounter - 1) + ", i1* " + register);
            }
        } else if (type == "int[]") {
            if (thisMethod.definedVars.containsKey(expressionResult)
                    || thisMethod.formalParams.containsKey(expressionResult)) {
                System.out.println(
                        "%_" + registerCounter + " = load i32*, i32** " + variableRegister.get(expressionResult));
                registerCounter++;
            }
            System.out.println("store i32* %_" + (registerCounter - 1) + ", i32** " + register);
        } else if (type == "boolean[]") {
            if (thisMethod.definedVars.containsKey(expressionResult)
                    || thisMethod.formalParams.containsKey(expressionResult)) {
                System.out.println(
                        "%_" + registerCounter + " = load i1*, i1** " + variableRegister.get(expressionResult));
                registerCounter++;
            }
            System.out.println("store i1* %_" + (registerCounter - 1) + ", i1** " + register);
        } else {
            if (thisMethod.definedVars.containsKey(expressionResult)
                    || thisMethod.formalParams.containsKey(expressionResult)) {
                System.out.println(
                        "%_" + registerCounter + " = load i8*, i8** " + variableRegister.get(expressionResult));
                registerCounter++;
            }
            System.out.println("store i8* %_" + (registerCounter - 1) + ", i8** " + register);
        }

        return null;
    }

    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(BooleanArrayAllocationExpression n, Object argu) throws Exception {
        String arraySize = n.f3.accept(this, argu);
        System.out.println("%_" + registerCounter + " = add i32 1, " + arraySize);
        registerCounter++;

        System.out.println("%_" + registerCounter + " = icmp sge i32 %_" + (registerCounter - 1) + ", 1");
        registerCounter++;

        System.out.println("br i1 %_" + (registerCounter - 1) + ", label %nsz_ok_" + labelCounter
                + ", label %nsz_err_" + labelCounter);

        System.out.println("nsz_err_" + labelCounter + ":");
        System.out.println("call void @throw_nsz()");
        System.out.println("br label %nsz_ok_" + labelCounter);

        System.out.println("nsz_ok_" + labelCounter + ":");

        System.out.println("%_" + registerCounter + " = call i8* @calloc(i32 %_" + (registerCounter - 2) + ", i32 1)");
        registerCounter++;

        System.out.println("%_" + registerCounter + " = bitcast i8* %_" + (registerCounter - 1) + " to i1*");
        registerCounter++;

        System.out.println("store i32 " + arraySize + ", i32* %_" + (registerCounter - 1));

        return "boolean[]";
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    @Override
    public String visit(IntegerArrayAllocationExpression n, Object argu) throws Exception {
        String arraySize = n.f3.accept(this, argu);
        method thisMethod = (method) argu;
        if (isVar(arraySize, thisMethod) != null) {
            arraySize = loadVarToRegister(arraySize, "int");
        }
        System.out.println("%_" + registerCounter + " = add i32 1, " + arraySize);
        registerCounter++;

        System.out.println("%_" + registerCounter + " = icmp sge i32 %_" + (registerCounter - 1) + ", 1");
        registerCounter++;

        System.out.println("br i1 %_" + (registerCounter - 1) + ", label %nsz_ok_" + labelCounter
                + ", label %nsz_err_" + labelCounter);

        System.out.println("nsz_err_" + labelCounter + ":");
        System.out.println("call void @throw_nsz()");
        System.out.println("br label %nsz_ok_" + labelCounter);
        System.out.println("nsz_ok_" + labelCounter + ":");

        System.out.println("%_" + registerCounter + " = call i8* @calloc(i32 %_" + (registerCounter - 2) + ", i32 4)");
        registerCounter++;

        System.out.println("%_" + registerCounter + " = bitcast i8* %_" + (registerCounter - 1) + " to i32*");
        registerCounter++;

        System.out.println("store i32 " + arraySize + ", i32* %_" + (registerCounter - 1));
        labelCounter++;
        return "int[]";
    }

    // /**
    // * f0 -> "new"
    // * f1 -> Identifier()
    // * f2 -> "("
    // * f3 -> ")"
    // */
    @Override
    public String visit(AllocationExpression n, Object argu) throws Exception {
        List<variable> variableList = null; // new ArrayList<variable>(map.values());
        classMap parentClass = null;
        String type = n.f1.accept(this, argu);
        int fieldOffset = 0;
        parentClass = classes.get(type).parentClass;
        if (classes.get(type).fieldOffset.length == 0) {
            while (parentClass != null) {
                if (parentClass.fieldOffset.length != 0) {
                    fieldOffset = parentClass.fieldOffset[parentClass.fieldOffset.length - 1];
                    variableList = new ArrayList<variable>(parentClass.fields.values());
                    if (variableList.get(parentClass.fieldOffset.length - 1).Type == "int") {
                        fieldOffset += 4;
                    } else if (variableList.get(parentClass.fieldOffset.length - 1).Type == "boolean") {
                        fieldOffset += 1;
                    } else {
                        fieldOffset += 8;
                    }
                    break;
                }
                parentClass = classes.get(type).parentClass;
            }
        } else {
            fieldOffset = classes.get(type).fieldOffset[classes.get(type).fieldOffset.length - 1];
            variableList = new ArrayList<variable>(classes.get(type).fields.values());
            if (variableList.get(classes.get(type).fieldOffset.length - 1).Type == "int") {
                fieldOffset += 4;
            } else if (variableList.get(classes.get(type).fieldOffset.length - 1).Type == "boolean") {
                fieldOffset += 1;
            } else {
                fieldOffset += 8;
            }
        }
        System.out.println("%_" + registerCounter + " = call i8* @calloc(i32 1, i32 " + fieldOffset + 8 + ")");
        registerCounter++;

        System.out.println("%_" + registerCounter + " = bitcast i8* %_" + (registerCounter - 1) + " to i8***");
        registerCounter++;

        System.out.println(
                "%_" + registerCounter + " = getelementptr [2 x i8*], [2 x i8*]* @." + type + "_vtable, i32 0, i32 0");
        registerCounter++;

        System.out.println("store i8** %_" + (registerCounter - 1) + ", i8*** %_" + (registerCounter - 2));

        return type;
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, Object argu) throws Exception {
        String type = n.f1.accept(this, argu);
        return type;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Object argu) throws Exception {
        String type = n.f1.accept(this, argu);
        type = isVar(type, (method) argu);
        if (!type.equals("boolean")) {
            throw new Exception("Exception: 'Not' on Non-Boolean expresion");
        }
        return type;
    }

    @Override
    public String visit(BooleanArrayType n, Object argu) {
        return "boolean[]";
    }

    @Override
    public String visit(IntegerArrayType n, Object argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, Object argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, Object argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Object argu) {
        return n.f0.toString();
    }

    @Override
    public String visit(IntegerLiteral n, Object argu) throws Exception {
        return n.f0.toString();
    }

    @Override
    public String visit(TrueLiteral n, Object argu) throws Exception {
        return n.f0.toString();
    }

    @Override
    public String visit(FalseLiteral n, Object argu) throws Exception {
        return n.f0.toString();
    }
}
