/*
 * $$Id$$
 * Copyright (c) 2011 Qunar.com. All Rights Reserved.
 */

package com.qunar.base.qunit.util;

import com.qunar.base.qunit.command.StepCommand;

/**
 * desc
 * Created by JarnTang at 12-8-16 下午4:17
 *
 * @author  JarnTang
 */
public class CommandUtil {

    public static StepCommand concatCommand(StepCommand oneCommand, StepCommand otherCommand) {
        if (oneCommand == null || otherCommand == null) {
            return oneCommand == null ? otherCommand : oneCommand;
        }
        StepCommand command = oneCommand;
        while (command.hasNextCommand()) {
            command = command.getNextCommand();
        }
        command.setNextCommand(otherCommand);
        return oneCommand;
    }

}
