/*
* $$Id$$
* Copyright (c) 2011 Qunar.com. All Rights Reserved.
*/
package com.qunar.qunit.sample.runner;

import com.qunar.base.qunit.Qunit;
import com.qunar.base.qunit.annotation.Options;
import org.junit.runner.RunWith;

/**
* Qunit Runner 测试入口
*
* Created by JarnTang at 12-5-21 下午1:30
*
* @author  JarnTang
*/
@RunWith(Qunit.class)
@Options(files = {"cases/*.xml"}, service = {"service.xml","service-local.xml"}, dsl= "cases/dsl.xml", tags="jingchao.mao")
public class RunnerTest {

}

