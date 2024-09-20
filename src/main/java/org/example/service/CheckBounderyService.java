package org.example.service;

import org.example.entity.SafeBoundery;
import org.example.enums.Operator;

public class CheckBounderyService {
    public boolean checkBoundery(SafeBoundery safeBoundery, String data){
        Operator operator = safeBoundery.getOperator();
        switch (operator){
            case operator.EQUAL:
                return Double.parseDouble(data) == safeBoundery.getValue1() || data.equals(safeBoundery.getString());
            case operator.GREATER_THAN:
                return Double.parseDouble(data) > safeBoundery.getValue2();
            case operator.LESS_THAN:
                return Double.parseDouble(data) < safeBoundery.getValue1();
            case operator.GREATER_THAN_OR_EQUAL:
                return Double.parseDouble(data) >= safeBoundery.getValue2();
            case operator.LESS_THAN_OR_EQUAL:
                return Double.parseDouble(data) <= safeBoundery.getValue1();
            case operator.BETWEEN:
                return Double.parseDouble(data) >= safeBoundery.getValue1() && Double.parseDouble(data) <= safeBoundery.getValue2();
            case operator.CONTAINS:
                return data.contains(safeBoundery.getString());
            default:
                return false;
        }
    }
}
