package de.thi.jbsa.examples;/*
 * de.thi.jbsa.examples.Examples
 * (c) Copyright BESK Kft, 2020
 * All Rights reserved.
 */

import static org.junit.Assert.assertEquals;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

/**
 * Created by Balazs Endredi <balazs.endredi@beskgroup.com> on 22.04.2020
 */
public class Examples {

  interface Calculator {

    String add(int a, int b);
  }

  interface MyInterface {

    String myMethod();
  }

  interface MyInterface2 {

    String myMethod1(String a);

    String myMethod2(int a, int b);
  }

  private class CalculatorImpl {

  }

  @Rule
  private ExpectedException expectedException = ExpectedException.none();

  private Calculator createCalculator() {
    Object target = new CalculatorImpl();

    Calculator calculatorProxy = (Calculator) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Calculator.class }, (proxy, method, args) -> invokeImpl(proxy, method, args));
    return calculatorProxy;
  }

  private Object invokeImpl(Object proxy, Method method, Object[] args) {
    return method.getName() +
      Stream.of(args != null ? args : new Object[0])
            .peek(arg -> invokeImpl_checkArg(arg))
            .map(String::valueOf)
            .collect(Collectors.joining(",", "(", ")"));
  }

  private void invokeImpl_checkArg(Object arg) {
    if (arg instanceof Integer && arg.equals(Integer.MAX_VALUE)) {
      throw new IllegalArgumentException("MAX int not allowed");
    }
  }

  @Test
  public void test_50_dynamic_proxy() {
    MyInterface myProxy = (MyInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { MyInterface.class }, (proxy, method, args) -> invokeImpl(proxy, method, args));
    assertEquals("myMethod()", myProxy.myMethod());
  }

  @Test
  public void test_51_dynamic_proxy() {
    MyInterface2 myProxy = (MyInterface2) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { MyInterface2.class }, (proxy, method, args) -> invokeImpl(proxy, method, args));
    assertEquals("myMethod1(hi)", myProxy.myMethod1("hi"));
    assertEquals("myMethod2(1,2)", myProxy.myMethod2(1, 2));
  }

  @Test
  public void test_52_dynamic_proxy() {
    Object myProxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { MyInterface.class, MyInterface2.class }, (proxy, method, args) -> invokeImpl(proxy, method, args));
    assertEquals("myMethod()", ((MyInterface) myProxy).myMethod());
    assertEquals("myMethod1(hi)", ((MyInterface2) myProxy).myMethod1("hi"));
    assertEquals("myMethod2(1,2)", ((MyInterface2) myProxy).myMethod2(1, 2));
  }

  @Test
  public void test_55_dynamic_proxy() {
    Calculator calculator = createCalculator();
    Assertions.assertThrows(IllegalArgumentException.class, () -> calculator.add(Integer.MAX_VALUE, 2));
  }
}
