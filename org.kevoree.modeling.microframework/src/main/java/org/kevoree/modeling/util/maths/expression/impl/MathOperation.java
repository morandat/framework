package org.kevoree.modeling.util.maths.expression.impl;

/**
 * Abstract definition of a supported operator. An operator is defined by
 * its name (pattern), precedence and if it is left- or right associative.
 */
public class MathOperation {

    private String oper;
    private int precedence;
    private boolean leftAssoc;

    public MathOperation(String oper, int precedence, boolean leftAssoc) {
        this.oper = oper;
        this.precedence = precedence;
        this.leftAssoc = leftAssoc;
    }

    public String getOper() {
        return oper;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean isLeftAssoc() {
        return leftAssoc;
    }


    public double eval(double v1, double v2){
        if(oper.equals("+")){
            return v1+v2;
        }
        else if(oper.equals("-")){
            return v1-v2;
        }
        else if(oper.equals("*")){
            return v1*v2;
        }
        else if(oper.equals("/")){
            return v1/v2;
        }
        else if(oper.equals("%")){
            return v1%v2;
        }
        else if(oper.equals("^")){
            return Math.pow(v1,v2);
        }
        else if(oper.equals("&&")){
            boolean b1 = !(v1==0);
            boolean b2 = !(v2==0);
            return b1 && b2 ? 1 : 0;
        }
        else if(oper.equals("||")){
            boolean b1 = !(v1==0);
            boolean b2 = !(v2==0);
            return b1 || b2 ? 1 : 0;
        }
        else if(oper.equals(">")){
            return v1>v2  ? 1 : 0;
        }
        else if(oper.equals(">=")){
            return v1>=v2  ? 1 : 0;
        }
        else if(oper.equals("<")){
            return v1<v2  ? 1 : 0;
        }
        else if(oper.equals("<=")){
            return v1<=v2  ? 1 : 0;
        }
        else if(oper.equals("==")){
            return v1==v2 ? 1 : 0;
        }
        else if(oper.equals("!=")){
            return v1 != v2 ? 1 : 0;
        }


        return 0;
    }
}