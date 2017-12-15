package com.qunar.base.qunit.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.qunar.base.qunit.annotation.ChildrenConfig;
import com.qunar.base.qunit.command.StepCommand;
import com.qunar.base.qunit.command.TearDownStepCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: zhaohuiyu
 * Date: 6/12/12
 * Time: 10:42 AM
 */
public abstract class CompositeStepConfig extends StepConfig {
    @ChildrenConfig
    private List<StepConfig> childrenConfig;

    protected final List<StepCommand> createChildren() {
        List<StepCommand> commands = new ArrayList<StepCommand>();
        for (StepConfig config : childrenConfig) {
            StepCommand command = config.createCommand();
            commands.add(command);
        }
        return commands;
    }

    protected final Map<String,List<StepCommand>> createChildrenWithTearDown() {
        Map<String,List<StepCommand>> commands = Maps.newHashMap();
        List<StepCommand> teardownCommmands = Lists.newArrayList();
        List<StepCommand> bodyCommmands = Lists.newArrayList();

        for (StepConfig config : childrenConfig) {
            StepCommand command = config.createCommand();
            if (command instanceof TearDownStepCommand) {
                teardownCommmands.add(command);
            }else {
                bodyCommmands.add(command);
            }
        }
        commands.put("body",bodyCommmands);
        commands.put("teardown",teardownCommmands);
        return commands;
    }
}
