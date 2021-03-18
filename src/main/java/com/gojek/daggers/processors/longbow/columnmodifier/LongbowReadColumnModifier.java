package com.gojek.daggers.processors.longbow.columnmodifier;

import java.util.ArrayList;
import java.util.Arrays;

import static com.gojek.daggers.utils.Constants.LONGBOW_PROTO_DATA;

public class LongbowReadColumnModifier implements ColumnModifier {

    @Override
    public String[] modifyColumnNames(String[] inputColumnNames) {
        ArrayList<String> inputColumnList = new ArrayList<>(Arrays.asList(inputColumnNames));
        inputColumnList.add(inputColumnList.size(), LONGBOW_PROTO_DATA);
        return inputColumnList.toArray(new String[0]);
    }
}