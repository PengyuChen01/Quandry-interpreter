package interpreter;
import java.io.*;
import java.util.Random;

import parser.ParserWrapper;
import ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interpreter {

    // Process return codes
    public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_PARSING_ERROR = 1;
    public static final int EXIT_STATIC_CHECKING_ERROR = 2;
    public static final int EXIT_DYNAMIC_TYPE_ERROR = 3;
    public static final int EXIT_NIL_REF_ERROR = 4;
    public static final int EXIT_QUANDARY_HEAP_OUT_OF_MEMORY_ERROR = 5;
    public static final int EXIT_DATA_RACE_ERROR = 6;
    public static final int EXIT_NONDETERMINISM_ERROR = 7;
    //private static HashMap<String, Long> variables = new HashMap<String, Long>();
    private static HashMap<String, FuncDef> function = new HashMap<String, FuncDef>();
    private static boolean returnFlag = false;
    static private Interpreter interpreter;
    public static Interpreter getInterpreter() {
        return interpreter;
    }

    public static void main(String[] args) {
        //args = new String[2];
        //args[0] = "examples/factsimple.q";
        //args[1] = "5";
        String gcType = "NoGC"; // default for skeleton, which only supports NoGC
        long heapBytes = 1 << 14;
        int i = 0;
        String filename;
        long quandaryArg;
        try {
            for (; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (arg.equals("-gc")) {
                        gcType = args[i + 1];
                        i++;
                    } else if (arg.equals("-heapsize")) {
                        heapBytes = Long.valueOf(args[i + 1]);
                        i++;
                    } else {
                        throw new RuntimeException("Unexpected option " + arg);
                    }
                } else {
                    if (i != args.length - 2) {
                        throw new RuntimeException("Unexpected number of arguments");
                    }
                    break;
                }
            }
            filename = args[i];
            quandaryArg = Long.valueOf(args[i + 1]);
        } catch (Exception ex) {
            System.out.println("Expected format: quandary [OPTIONS] QUANDARY_PROGRAM_FILE INTEGER_ARGUMENT");
            System.out.println("Options:");
            System.out.println("  -gc (MarkSweep|Explicit|NoGC)");
            System.out.println("  -heapsize BYTES");
            System.out.println("BYTES must be a multiple of the word size (8)");
            return;
        }

        Program astRoot = null;
        Reader reader;
        try {
            reader = new BufferedReader(new FileReader(filename));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try {
            astRoot = ParserWrapper.parse(reader);
        } catch (Exception ex) {
            ex.printStackTrace();
            Interpreter.fatalError("Uncaught parsing error: " + ex, Interpreter.EXIT_PARSING_ERROR);
        }
        //astRoot.println(System.out);
        interpreter = new Interpreter(astRoot);
        interpreter.initMemoryManager(gcType, heapBytes);
       
        String returnValueAsString = interpreter.executeRoot(astRoot, quandaryArg).toString();
        System.out.println("Interpreter returned " + returnValueAsString);
    }
  
    final Program astRoot;
    final Random random;

    private Interpreter(Program astRoot) {
        this.astRoot = astRoot;
        this.random = new Random();
    }

    void initMemoryManager(String gcType, long heapBytes) {
        if (gcType.equals("Explicit")) {
            throw new RuntimeException("Explicit not implemented");            
        } else if (gcType.equals("MarkSweep")) {
            throw new RuntimeException("MarkSweep not implemented");            
        } else if (gcType.equals("RefCount")) {
            throw new RuntimeException("RefCount not implemented");            
        } else if (gcType.equals("NoGC")) {
            // Nothing to do
        }
    }
    
    Object executeRoot(Program astRoot, long arg) {
        return evaluate(astRoot.getFuncDefList(),arg);
    }
  

    Object evaluate(FuncDefList funcDefList, long arg) {
        ArrayList<Long> args = new ArrayList<>();
        args.add(arg);
        FuncDef mainFunc = funcDefList.getFuncDef();
        String id = funcDefList.getFuncDef().getVarDecl().getId().getIdent();
        if (id.equals("main")) {
            mainFunc = funcDefList.getFuncDef();
        }
        //System.out.println(id);
        function.put(id, funcDefList.getFuncDef());
        while (funcDefList.getFuncDefList() != null) {
            funcDefList = funcDefList.getFuncDefList();
            id = funcDefList.getFuncDef().getVarDecl().getId().getIdent();
            //System.out.println(id);
            if (id.equals("main")) {
                mainFunc = funcDefList.getFuncDef();
            }
            function.put(id, funcDefList.getFuncDef());
        }
        
        // If a 'main' function was found, evaluate it.
        if (mainFunc.getVarDecl().getId().getIdent().equals("main")) {
            Map<String, Long> variablesMap = new HashMap<>();
            return evaluate(mainFunc, args, variablesMap);
        } else {
            throw new RuntimeException("No main method found");
        }
    }
    
    Long evaluate(FuncDef funcDef, ArrayList<Long> args, Map<String, Long> variablesMap) {
        if (funcDef.getFormalDeclList() != null) {
            evaluate(funcDef.getFormalDeclList(), args, variablesMap);
        }
        //returnFlag = false;
        return evaluate(funcDef.getStmtList(), variablesMap);

    
    }
    Long evaluate(StmtList stmtList, Map<String, Long> variablesMap){
        //System.out.println(stmtList.getStmt().toString());
        returnFlag = false;
        Long stmt = evaluate(stmtList.getStmt(), variablesMap);

        if (returnFlag == true) {
            return stmt;
        }
        //System.out.println("this is statement: " + stmtList.getStmt().toString());
        while (stmtList.getStmtList() != null)
        {
            stmtList = stmtList.getStmtList();
            
            //System.out.println(stmtList.getStmt().toString());
            stmt = evaluate(stmtList.getStmt(),variablesMap);
            
            if(returnFlag == true){
                break;
            }
        }
        return stmt;
    }
    Object evaluate(Type type){
        return evaluateType(type);
    }

    void evaluate(FormalDeclList formalDeclList, ArrayList<Long> args, Map<String, Long> variablesMap){
        evaluate(formalDeclList.getNeFormalDeclListNode(), args, variablesMap);
    }
    void evaluate(NeFormalDeclList neFormalDeclList, ArrayList<Long> arg, Map<String, Long> variablesMap){
        int idx = 0;
        variablesMap.put(neFormalDeclList.getVarDecl().getId().getIdent(), arg.get(idx));
        while (neFormalDeclList.getNeFormalDeclListNode() != null) {
            neFormalDeclList = neFormalDeclList.getNeFormalDeclListNode();
            idx++;
            variablesMap.put(neFormalDeclList.getVarDecl().getId().getIdent(), arg.get(idx));
        }
    }
    
    Long evaluate(ExprList exprList, Map<String, Long> variablesMap, ArrayList<Long> args){
        Long value = evaluate(exprList.getneExprList(),variablesMap, args);
       if (exprList.getneExprList() != null){
           return evaluate(exprList.getneExprList(),variablesMap, args);   
       }
       return value;
    }
    
    
    Long evaluate(NeExprList neExprList, Map<String, Long> variablesMap, ArrayList<Long> args){
        Long firstExprValue = evaluate(neExprList.getExpr(),variablesMap);
    
        if (neExprList.getneExprList() != null) {
            return evaluate(neExprList.getneExprList(), variablesMap, args);
        }
        return firstExprValue; 
    }
    

    boolean evaluateCond(Cond cond,Map<String, Long> variablesMap){
           switch(cond.getConditionOperator()){
                case Cond.LESSTHANOREQUAL: return evaluate(cond.getExpr1(),variablesMap) <= evaluate(cond.getExpr2(),variablesMap);
                case Cond.GREATERTHANOREQUAL: return evaluate(cond.getExpr1(),variablesMap) >= evaluate(cond.getExpr2(),variablesMap);
                case Cond.EQUALTO: return evaluate(cond.getExpr1(),variablesMap) == evaluate(cond.getExpr2(),variablesMap);
                case Cond.NOTEQUALTO: return evaluate(cond.getExpr1(),variablesMap) != evaluate(cond.getExpr2(),variablesMap);
                case Cond.LESSTHAN: return evaluate(cond.getExpr1(),variablesMap) < evaluate(cond.getExpr2(),variablesMap);
                case Cond.GREATERTHAN: return evaluate(cond.getExpr1(),variablesMap) > evaluate(cond.getExpr2(),variablesMap);
                case Cond.AND: return evaluateCond((Cond)cond.getExpr1(),variablesMap) && evaluateCond((Cond)cond.getExpr2(),variablesMap);
                case Cond.OR: return evaluateCond((Cond)cond.getExpr1(),variablesMap) || evaluateCond((Cond)cond.getExpr2(),variablesMap);
                case Cond.NOT: return !(evaluateCond((Cond)cond.getExpr1(),variablesMap));

                default: throw new RuntimeException("Unhandled operator");
              
           }
    
    }
    Long evaluate(Stmt stmt, Map<String, Long> variablesMap){
        if (stmt instanceof DeclarationStmt){
            DeclarationStmt declStmt = (DeclarationStmt)stmt;
            String varName = declStmt.getVarDecl().getId().getIdent();
            Long value = evaluate(declStmt.getExpression(),variablesMap);
            variablesMap.put(varName, value);
            return value;
        }
        else if (stmt instanceof AssignmentStmt) {
            AssignmentStmt assignStmt = (AssignmentStmt)stmt;
            String varName = assignStmt.getIdNode().getIdent();
            Long value = evaluate(assignStmt.getExprNode(),variablesMap);
            
            // Check if variable is declared, if not, declare it
            if (!variablesMap.containsKey(varName)) {
                variablesMap.put(varName, value);
            }
            // Assign value to variable
            variablesMap.put(varName, value);
            return value;
        } else if (stmt instanceof IfStatement) {
            IfStatement ifStatement = (IfStatement)stmt;
            
            boolean condition = evaluateCond(ifStatement.getCondNode(),variablesMap);
            if (condition){
                return evaluate(ifStatement.getStmtNode(), variablesMap);
            }
           
            //System.out.println(value);
            return null;
        }
         
        else if (stmt instanceof IfElseStmt){
            IfElseStmt ifElseStmt = (IfElseStmt)stmt;
        
            boolean condition = evaluateCond(ifElseStmt.getCondNode(),variablesMap);
            
            Long value = null;
            if (condition){
                value = evaluate(ifElseStmt.getStmtNode1(), variablesMap);
            } else {
                value = evaluate(ifElseStmt.getStmtNode2(),variablesMap);
            }
            return value;
        }
        
         else if (stmt instanceof WhileStmt) {
            WhileStmt whileStmt = (WhileStmt)stmt;
            Boolean condition = evaluateCond(whileStmt.getCondNode(),variablesMap);
            Long value = evaluate(whileStmt.getStmtNode(),variablesMap);
            
            while (condition) {
                value = evaluate(whileStmt.getStmtNode(),variablesMap);
                condition = evaluateCond(whileStmt.getCondNode(),variablesMap);
            }
            return value;
        }
        else if (stmt instanceof CallStmt) {
            CallStmt callStmt = (CallStmt)stmt;
            ArrayList<Long> args = new ArrayList<>();
            
           
            if (callStmt.getExprList() != null) {
                NeExprList neExprList = callStmt.getExprList().getneExprList();
                args.add(evaluate(neExprList.getExpr(), variablesMap));
                while (neExprList.getneExprList() != null) {
                    neExprList = neExprList.getneExprList();
                    args.add(evaluate(neExprList.getExpr(), variablesMap));
                   
                }
            }
            return evaluate(callStmt.getExprList(), variablesMap,args);
        }
        
         else if (stmt instanceof PrintStmt) {
            PrintStmt printStmt = (PrintStmt)stmt;
            Long value = evaluate(printStmt.getExpression(),variablesMap);
            System.out.println(value);
            return value;
        }else if (stmt instanceof ReturnStmt) {
            ReturnStmt returnStmt = (ReturnStmt)stmt;
            
            Long value = evaluate(returnStmt.getExpression(),variablesMap);
            returnFlag = true;
            //System.out.println("return value: " + value);
            //System.out.println(value);
            return value;
        }else if (stmt instanceof BlockStmt) {
            BlockStmt blockStmt = (BlockStmt)stmt;
            
            Long value = evaluate(blockStmt.getStmtListNode(), variablesMap);
            
            return value;
        }
         else {
            throw new RuntimeException("Unhandled Stmt type");
        }
    }

    Long evaluate(Expr expr, Map<String, Long> variablesMap) {
        if (expr instanceof ConstExpr) {
            return ((ConstExpr)expr).getValue();
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr)expr;
            switch (binaryExpr.getOperator()) {
                case BinaryExpr.PLUS: return evaluate(binaryExpr.getLeftExpr(),variablesMap) + evaluate(binaryExpr.getRightExpr(),variablesMap);
                case BinaryExpr.MINUS: return evaluate(binaryExpr.getLeftExpr(),variablesMap) - evaluate(binaryExpr.getRightExpr(),variablesMap);
                case BinaryExpr.TIMES: return evaluate(binaryExpr.getLeftExpr(),variablesMap) * evaluate(binaryExpr.getRightExpr(),variablesMap);
                default: throw new RuntimeException("Unhandled operator");
            }
        } else if (expr instanceof UnaryMinusExpr){
            Expr child = ((UnaryMinusExpr)expr).getExpr();
            long value = (long) evaluate(child, variablesMap);
            long newValue = -value;
            return newValue;
        } else if(expr instanceof IdentExpr){            
            return variablesMap.get(((IdentExpr)expr).getIdent());
        } else if(expr instanceof CallExpr){
            ArrayList<Long> args = new ArrayList<>();
            //System.out.println(((CallExpr)expr).getExprList().toString());
            if (((CallExpr)expr).getExprList() != null) {
                NeExprList neExprList = ((CallExpr)expr).getExprList().getneExprList();
                args.add((long)evaluate(neExprList.getExpr(), variablesMap));
                //System.out.println(args.get(0));
                while (neExprList.getneExprList() != null) {
                    neExprList = neExprList.getneExprList();
                    args.add((long)evaluate(neExprList.getExpr(), variablesMap));
                    //System.out.println(args.get(1));
                }
            }

            if (((CallExpr)expr).getId().getIdent().equals("randomInt")) {
                Random random = new Random();
                return (long)random.nextInt((args.get(0)).intValue());
            }
            Map<String, Long> tempMap = new HashMap<>(variablesMap);
            //System.out.println(function.get(((CallExpr)expr).getId().getIdent()));
            Long result = evaluate(function.get(((CallExpr)expr).getId().getIdent()), args, tempMap);
            returnFlag = false;
            return result;
           // return evaluate(function.get(functionName).getStmtList());
        }
        else {
            throw new RuntimeException("Unhandled Expr type");
        }
    }
    Object evaluateType(Type type){
        return type.getType();
    }
	public static void fatalError(String message, int processReturnCode) {
        System.out.println(message);
        System.exit(processReturnCode);
	}
}
