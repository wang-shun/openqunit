package com.qunar.base.validator.schema;

import java.util.Arrays;
import java.util.List;

public class Attr {

	String attrName;
	List<Object> arguments;

    public Attr(String attrName,List<Object> arguments){
		this.attrName = attrName;
		this.arguments = arguments;
	}
	
	public String getAttrName() {
		return attrName;
	}

    public List<Object> getArguments() {
		return arguments;
	}

    @Override
	public String toString() {
		return "[attrName="+attrName+",arguments="+(arguments == null ? null : Arrays.toString(arguments.toArray()))+"]";
	}
	
	
	
}
