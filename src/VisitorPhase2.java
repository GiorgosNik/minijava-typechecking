package src;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;

public class VisitorPhase2 extends GJDepthFirst<String, Object> {
    public LinkedHashMap<String, classMap> classes;

    public String isVal(String name, method currMethod) throws Exception {
        // Use this function, to retrieve a variable's or Object's type, given its name and scope
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
            if (currMethod.belongsTo.parentClass.fields.containsKey(name)) {
                type = currMethod.belongsTo.parentClass.fields.get(name).Type;
            } else {
                throw new Exception("Exception: Variable does not exist");
            }
        } else {
            throw new Exception("Exception: Variable does not exist");
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
        // Expand and check any calls and declarations in the main class
        classMap thisClass = classes.get(n.f1.accept(this, argu));
        String name = "main";
        method thisMethod = thisClass.methods.get(name);
        n.f15.accept(this, thisMethod);
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
    public String visit(MethodDeclaration n, Object argu) throws Exception {
        classMap thisClass = (classMap) argu;
        String type = n.f1.accept(this, argu);
        String name = n.f2.accept(this, argu);
        method thisMethod = thisClass.methods.get(name);
        String returnType = n.f10.accept(this, thisMethod);
        // Check method returns a known type
        if (!(returnType.equals("boolean") || returnType.equals("int") || returnType.equals("boolean[]")
                || returnType.equals("int[]"))) {
            returnType = isVal(returnType, thisMethod);
        }
        // Check the returned value actually is of the same type
        if (!type.equals(returnType)) {
            throw new Exception("Exception: Return Type does not match returned value");
        }

        n.f8.accept(this, thisMethod);
        return null;
    }

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, Object argu) throws Exception {
        // Get the type of the item referred to by 'this'
        if (argu.getClass() == method.class) {
            return ((method) argu).belongsTo.Name;
        } else if (argu.getClass() == classMap.class) {
            return ((classMap) argu).Name;
        } else {
            throw new Exception("Exception: Bad use of 'THIS'");
        }
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Object argu) throws Exception {
        String name = n.f0.accept(this, argu);
        String type = isVal(name, (method) argu);
        String exprType = n.f2.accept(this, argu);
        // Check the expression is of a known type
        if (!(exprType.equals("int") || exprType.equals("boolean") || exprType.equals("int[]")
                || exprType.equals("boolean[]")) && !classes.containsKey(exprType)) {
            exprType = isVal(exprType, (method) argu);
        }

        // Check the variable is of the same type, or of the parent class if applicable
        if (!exprType.equals(type)) {
            if (classes.containsKey(exprType)) {
                if (classes.get(exprType).parentClass != null) {
                    if (!classes.get(exprType).parentClass.Name.equals(type)) {
                        throw new Exception("Exception: Incomatible Assignement");
                    }
                } else {
                    throw new Exception("Exception: Incomatible Assignement");
                }
            } else {
                throw new Exception("Exception: Incomatible Assignement");
            }
        }
        return null;
    }

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
        String arrayType = isVal(arrayName, (method) argu);
        arrayType = isVal(arrayType, (method) argu);

        // Check that the identifier is indeed of array-type
        if (!(arrayType.equals("int[]") || arrayType.equals("boolean[]"))) {
            throw new Exception("Exception: Array Assignement to Non-Array Type");
        }

        // Check that the index is of type integer
        String indexType = n.f2.accept(this, argu);
        indexType = isVal(indexType, (method) argu);
        if (!indexType.equals("int")) {
            throw new Exception("Exception: Array Assignement with Non-Integer Index");
        }
        String exprType = n.f5.accept(this, argu);
        exprType = isVal(exprType, (method) argu);

        //Check that the value to assign to the array is of the correct type
        if (arrayType.equals("int[]") && exprType.equals("int")) {
            return null;
        } else if (arrayType.equals("boolean[]") && exprType.equals("boolean")) {
            return null;
        } else {
            throw new Exception("Exception: Assignement of incompatible type to array");
        }
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
        String _ret = null;
        String ifCondtion = n.f2.accept(this, argu);
        ifCondtion = isVal(ifCondtion, (method) argu);
        // Check that the condition has boolean value
        if (!ifCondtion.equals("boolean")) {
            throw new Exception("Exception: If statement has Non-Boolean Condition");
        }
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
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
        String loopCondtion = n.f2.accept(this, argu);
        loopCondtion = isVal(loopCondtion, (method) argu);
        if (!loopCondtion.equals("boolean")) {
            throw new Exception("Exception: Loop has Non-Boolean Condition");
        }
        n.f4.accept(this, argu);
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
        String type = n.f2.accept(this, argu);
        type = isVal(type, (method) argu);
        // Check that the condition has boolean value
        if (!(type.equals("int") || type.equals("boolean"))) {
            throw new Exception("Exception: Cant print this object");
        }
        return null;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, Object argu) throws Exception {
        String className = n.f0.accept(this, argu);
        method toCall;

        // Check that the object or class the method is called on, actually exists
        if ((!classes.containsKey(className)) && (!(((method) argu).formalParams.containsKey(className)
                || ((method) argu).definedVars.containsKey(className)))) {
            if (!((method) argu).belongsTo.fields.containsKey(className)) {
                if (((method) argu).belongsTo.parentClass != null) {
                    if (!((method) argu).belongsTo.parentClass.fields.containsKey(className)) {
                        throw new Exception("Exception: No such class or variable");
                    }
                } else {
                    throw new Exception("Exception: No such class or variable");
                }
            }
        }

        // If the method is inherited, set the className to be the one of the parent class for future reference
        if (argu != null && !classes.containsKey(className)) {
            if (((method) argu).formalParams.containsKey(className)) {
                className = ((method) argu).formalParams.get(className).Type;
            } else if (((method) argu).definedVars.containsKey(className)) {
                className = ((method) argu).definedVars.get(className).Type;
            } else if (((method) argu).belongsTo.fields.containsKey(className)) {
                className = ((method) argu).belongsTo.fields.get(className).Type;
            } else if (((method) argu).belongsTo.parentClass.fields.containsKey(className)) {
                className = ((method) argu).belongsTo.parentClass.fields.get(className).Type;
            }
        }

        // Check the method belongs to the class
        String funcName = n.f2.accept(this, argu);
        if (classes.get(className).methods.containsKey(funcName)) {
            toCall = classes.get(className).methods.get(funcName);
        } else if (classes.get(className).parentClass != null) {
            if (classes.get(className).parentClass.methods.containsKey(funcName)) {
                toCall = classes.get(className).parentClass.methods.get(funcName);
            } else {
                throw new Exception("Exception: No such method");
            }
        } else {
            System.out.println(className + " " + funcName);
            throw new Exception("Exception: No such method");
        }

        // Check that the arguments and the formal parameters match
        // First get both, and compare their size
        String typeString = "" + n.f4.accept(this, argu);
        if (typeString.length() == 0 && !toCall.formalParams.isEmpty()) {
            throw new Exception("Exception: Arguments do not match");
        }
        String[] parts = typeString.split(",");
        if (parts.length != toCall.formalParams.size() && !toCall.formalParams.isEmpty()) {
            throw new Exception("Exception: Arguments do not match");
        }

        // If they are the same size, check the type of each argument in order
        List<variable> formalParams = new ArrayList<variable>(toCall.formalParams.values());
        for (int i = 0; i < formalParams.size(); i++) {
            if (!(formalParams.get(i).Type.equals(isVal(parts[i], ((method) argu))))) {
                if (classes.containsKey(formalParams.get(i).Type)) {
                    if (classes.containsKey(isVal(parts[i], ((method) argu)))) {
                        if (!classes.get(isVal(parts[i], ((method) argu))).parentClass.Name
                                .equals(formalParams.get(i).Type)) {
                                    System.out.println(classes.get(isVal(parts[i], ((method) argu))).parentClass.Name+" "+formalParams.get(i).Type);
                                    if(!(classes.get(isVal(parts[i], ((method) argu))).parentClass.parentClass.Name.equals(formalParams.get(i).Type))){
                                        throw new Exception("Exception: Arguments do not match");
                                    }
                        }
                    } else {
                        throw new Exception("Exception: Arguments do not match");
                    }
                } else {
                    throw new Exception("Exception: Arguments do not match");
                }
            }
        }
        return toCall.Type;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    @Override
    public String visit(ExpressionList n, Object argu) throws Exception {
        String typeString = n.f0.accept(this, argu);
        if (n.f1 != null) {
            typeString += n.f1.accept(this, argu);
        }
        return typeString;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    @Override
    public String visit(ExpressionTail n, Object argu) throws Exception {
        String ret = "";
        for (Node node : n.f0.nodes) {
            ret += "," + node.accept(this, argu);
        }
        return ret;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    @Override
    public String visit(ExpressionTerm n, Object argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, Object argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        type1 = isVal(type1, (method) argu);
        String type2 = n.f2.accept(this, argu);
        type2 = isVal(type2, (method) argu);
        if (!(type1.equals("boolean") && type2.equals("boolean"))) {
            throw new Exception("Exception: And operation on Non-Boolean");
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, Object argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        type1 = isVal(type1, (method) argu);
        String type2 = n.f2.accept(this, argu);
        type2 = isVal(type2, (method) argu);
        if (!(type1.equals("int") && type2.equals("int"))) {
            throw new Exception("Exception: Comparison of Non Int Elements");
        }
        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, Object argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        type1 = isVal(type1, (method) argu);
        String type2 = n.f2.accept(this, argu);
        type2 = isVal(type2, (method) argu);
        if (!(type1.equals("int") && type2.equals("int"))) {
            throw new Exception("Exception: Addition of Non Int Elements");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, Object argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        type1 = isVal(type1, (method) argu);
        String type2 = n.f2.accept(this, argu);
        type2 = isVal(type2, (method) argu);
        if (!(type1.equals("int") && type2.equals("int"))) {
            throw new Exception("Exception: Subtraction of Non Int Elements");
        }
        return "int";
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
        String arrayType = isVal(arrayName, (method) argu);
        if (!(arrayType.equals("int[]") || arrayType.equals("boolean[]"))) {
            throw new Exception("Exception: Array Lookup in Non-Array Expression");
        }
        String indexName = n.f2.accept(this, argu);
        String indexType = isVal(indexName, (method) argu);
        if (!indexType.equals("int")) {
            throw new Exception("Exception: Array Lookup with Non-Integer Index");
        }
        if (arrayType.equals("int[]")) {
            return "int";
        } else {
            return "boolean";
        }
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    @Override
    public String visit(Clause n, Object argu) throws Exception {
        String type = n.f0.accept(this, argu);
        return type;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, Object argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        type1 = isVal(type1, (method) argu);
        String type2 = n.f2.accept(this, argu);
        type2 = isVal(type2, (method) argu);
        if (!(type1.equals("int") && type2.equals("int"))) {
            throw new Exception("Exception: Multiplication of Non Int Elements");
        }
        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Object argu) throws Exception {
        String type = n.f0.accept(this, argu);
        type = isVal(type, (method) argu);
        if (!(type.equals("boolean[]") || type.equals("int[]"))) {
            throw new Exception("Exception: Length on Non-Array Object");
        }
        return "int";
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, Object argu) throws Exception {
        String type = n.f0.accept(this, argu);
        return type;
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
        String expressionType = n.f3.accept(this, argu);
        expressionType = isVal(expressionType, (method) argu);
        if (!expressionType.equals("int")) {
            throw new Exception("Exception: Non-Integer expression in array allocation");
        }
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
        String expressionType = n.f3.accept(this, argu);
        expressionType = isVal(expressionType, (method) argu);
        if (!expressionType.equals("int")) {
            throw new Exception("Exception: Non-Integer expression in array allocation");
        }
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Object argu) throws Exception {
        String type = n.f1.accept(this, argu);
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
        type = isVal(type, (method) argu);
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
        return "int";
    }

    @Override
    public String visit(TrueLiteral n, Object argu) throws Exception {
        return "boolean";
    }

    @Override
    public String visit(FalseLiteral n, Object argu) throws Exception {
        return "boolean";
    }
}
