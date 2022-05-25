all: compile

compile:
	java -jar ./jtb132di.jar -te ./minijava/minijava.jj
	java -jar ./javacc5.jar ./minijava/minijava-jtb.jj
	javac ./src/variable.java
	javac ./src/method.java
	javac ./src/classMap.java
	javac ./src/argumentListType.java
	javac ./src/VisitorPhase1.java
	javac ./src/VisitorPhase2.java
	javac ./src/VisitorPhase3.java
	javac ./Main.java

clean:
	rm -f *.class *~
	rm -f TokenMgrError.java
	rm -f TokenMgrError.java
	rm -f TokenMgrError.java
	rm -f JavaCharStream
	rm -f JavaCharStream.java
	rm -f Token
	rm -f Token.java
	rm -f MiniJavaParser.java
	rm -f MiniJavaParserConstants.java
	rm -f ParseException
	rm -f ParseException.java
	rm -f MiniJavaParserTokenManager.java
	rm -f MiniJavaParserTokenManager
	rm -f -r ./syntaxtree
	rm -f -r ./visitor


