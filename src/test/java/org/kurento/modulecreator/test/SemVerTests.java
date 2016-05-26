/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.kurento.modulecreator.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.zafarkhaja.semver.Parser;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;

public class SemVerTests {

  @Test
  public void test() {

    // Wildcard - 1.* which is equivalent to >=1.0.0 & <2.0.0
    // Tilde operator - ~1.5 which is equivalent to >=1.5.0 & <2.0.0
    // Range - 1.0-2.0 which is equivalent to >=1.0.0 & <=2.0.0
    // Negation operator - !(1.*) which is equivalent to <1.0.0 & >=2.0.0
    // Short notation - 1 which is equivalent to =1.0.0
    // Parenthesized expression - ~1.3 | (1.4.* & !=1.4.5) | ~2

    assertThat(Version.valueOf("1.0.0-beta").satisfies(">=1.0.0 & <2.0.0"), is(false));

    assertThat(Version.valueOf("1.5.0").satisfies(">=1.0.0 & < 2.0.0"), is(true));

    assertThat(Version.valueOf("1.5.0").satisfies("~1.0"), is(true));

  }

  @Test
  public void parserTest() {

    Parser<Expression> parser = ExpressionParser.newInstance();

    Expression eq = parser.parse("=1.0.0");
    assertTrue(eq.interpret(Version.valueOf("1.0.0")));

    Expression eq2 = parser.parse("1.0.0");
    assertTrue(eq2.interpret(Version.valueOf("1.0.0")));

    Expression ne = parser.parse("!=1.0.0");
    assertTrue(ne.interpret(Version.valueOf("1.2.3")));

    Expression gt = parser.parse(">1.0.0");
    assertTrue(gt.interpret(Version.valueOf("1.2.3")));

    Expression ge = parser.parse(">=1.0.0");
    assertTrue(ge.interpret(Version.valueOf("1.0.0")));
    assertTrue(ge.interpret(Version.valueOf("1.2.3")));

    Expression lt = parser.parse("<1.2.3");
    assertTrue(lt.interpret(Version.valueOf("1.0.0")));

    Expression le = parser.parse("<=1.2.3");
    assertTrue(le.interpret(Version.valueOf("1.0.0")));
    assertTrue(le.interpret(Version.valueOf("1.2.3")));

    Expression expr1 = parser.parse("~1");
    assertTrue(expr1.interpret(Version.valueOf("1.2.3")));
    assertTrue(expr1.interpret(Version.valueOf("3.2.1")));

    Expression expr2 = parser.parse("~1.2");
    assertTrue(expr2.interpret(Version.valueOf("1.2.3")));
    assertFalse(expr2.interpret(Version.valueOf("2.0.0")));

    Expression expr3 = parser.parse("~1.2.3");
    assertTrue(expr3.interpret(Version.valueOf("1.2.3")));
    assertFalse(expr3.interpret(Version.valueOf("1.3.0")));

    Expression expr4 = parser.parse("1");
    assertTrue(expr4.interpret(Version.valueOf("1.0.0")));

    Expression expr5 = parser.parse("2.0");
    assertTrue(expr5.interpret(Version.valueOf("2.0.0")));

    Expression expr6 = parser.parse("1.*");
    assertTrue(expr6.interpret(Version.valueOf("1.2.3")));
    assertFalse(expr6.interpret(Version.valueOf("3.2.1")));

    Expression expr7 = parser.parse("1.2.*");
    assertTrue(expr7.interpret(Version.valueOf("1.2.3")));
    assertFalse(expr7.interpret(Version.valueOf("1.3.2")));

    Expression range = parser.parse("1.0.0 - 2.0.0");
    assertTrue(range.interpret(Version.valueOf("1.2.3")));
    assertFalse(range.interpret(Version.valueOf("3.2.1")));

    Expression and = parser.parse(">=1.0.0 & <2.0.0");
    assertTrue(and.interpret(Version.valueOf("1.2.3")));
    assertFalse(and.interpret(Version.valueOf("3.2.1")));

    Expression or = parser.parse("1.* | =2.0.0");
    assertTrue(or.interpret(Version.valueOf("1.2.3")));
    assertFalse(or.interpret(Version.valueOf("2.1.0")));

    Expression expr8 = parser.parse("(1)");
    assertTrue(expr8.interpret(Version.valueOf("1.0.0")));
    assertFalse(expr8.interpret(Version.valueOf("2.0.0")));

    Expression expr9 = parser.parse("((1))");
    assertTrue(expr9.interpret(Version.valueOf("1.0.0")));
    assertFalse(expr9.interpret(Version.valueOf("2.0.0")));

    Expression not1 = parser.parse("!(1)");
    assertTrue(not1.interpret(Version.valueOf("2.0.0")));
    assertFalse(not1.interpret(Version.valueOf("1.0.0")));

    Expression not2 = parser.parse("0.* & !(>=1 & <2)");
    assertTrue(not2.interpret(Version.valueOf("0.5.0")));
    assertFalse(not2.interpret(Version.valueOf("1.0.1")));

    Expression not3 = parser.parse("!(>=1 & <2) & >=2");
    assertTrue(not3.interpret(Version.valueOf("2.0.0")));
    assertFalse(not3.interpret(Version.valueOf("1.2.3")));

    Expression expr10 = parser.parse("(~1.0 & <2.0) | >2.0");
    assertTrue(expr10.interpret(Version.valueOf("2.5.0")));
    Expression expr11 = parser.parse("~1.0 & (<2.0 | >2.0)");
    assertFalse(expr11.interpret(Version.valueOf("2.5.0")));

    Expression expr12 = parser.parse("((>=1.0.1 & <2) | (>=3.0 & <4)) & ((1-1.5) & (~1.5))");
    assertTrue(expr12.interpret(Version.valueOf("1.5.0")));
    assertFalse(expr12.interpret(Version.valueOf("2.5.0")));
  }
}
