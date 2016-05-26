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
package org.kurento.modulecreator;

import com.github.zafarkhaja.semver.Parser;
import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.And;
import com.github.zafarkhaja.semver.expr.Equal;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.github.zafarkhaja.semver.expr.Greater;
import com.github.zafarkhaja.semver.expr.GreaterOrEqual;
import com.github.zafarkhaja.semver.expr.Less;
import com.github.zafarkhaja.semver.expr.LessOrEqual;
import com.github.zafarkhaja.semver.expr.Or;

public class VersionManager {

  private static final String DEV_SUFFIX = "-dev";
  private static final int DEV_SUFFIX_LENGTH = DEV_SUFFIX.length();

  public static String convertToMavenImport(String version) {

    if (isDevelopmentVersion(version)) {
      return removeDevSuffix(version) + "-SNAPSHOT";
    }

    Expression expression = parseVersion(version);

    String mavenVersion = convertToMavenExpression(expression);

    if (mavenVersion == null) {
      throw new KurentoModuleCreatorException("Version '" + version + "' in import not supported");
    }

    return mavenVersion;
  }

  private static String convertToMavenExpression(Expression expression) {

    if (expression instanceof Equal) {
      return ((Equal) expression).getParsedVersion().toString();
    } else if (expression instanceof Less) {
      return "(," + ((Less) expression).getParsedVersion() + "-SNAPSHOT)";
    } else if (expression instanceof LessOrEqual) {
      return "(," + ((LessOrEqual) expression).getParsedVersion() + "]";
    } else if (expression instanceof Greater) {
      return "(" + ((Greater) expression).getParsedVersion() + ",)";
    } else if (expression instanceof GreaterOrEqual) {
      return "[" + ((GreaterOrEqual) expression).getParsedVersion() + ",)";
    } else if (expression instanceof And) {

      And and = (And) expression;
      Expression left = and.getLeft();
      Expression right = and.getRight();

      Expression greater = getGreater(left, right);
      Expression less = getLess(left, right);

      if (greater == null || less == null) {
        return null;
      }

      StringBuilder mavenVersion = new StringBuilder();
      Version greaterVersion = null;
      if (greater instanceof Greater) {
        mavenVersion.append("(");
        greaterVersion = ((Greater) greater).getParsedVersion();
      } else {
        mavenVersion.append("[");
        greaterVersion = ((GreaterOrEqual) greater).getParsedVersion();
      }
      mavenVersion.append(greaterVersion).append(",");

      Version lessVersion = null;
      String postFix;
      if (less instanceof Less) {
        postFix = "-SNAPSHOT)";
        lessVersion = ((Less) less).getParsedVersion();
      } else {
        postFix = "]";
        lessVersion = ((LessOrEqual) less).getParsedVersion();
      }
      mavenVersion.append(lessVersion);
      mavenVersion.append(postFix);

      return mavenVersion.toString();

    } else if (expression instanceof Or) {
      String left = convertToMavenExpression(((Or) expression).getLeft());
      String right = convertToMavenExpression(((Or) expression).getRight());

      if (left != null && right != null) {
        return left + "," + right;
      }
    }

    return null;
  }

  private static Expression getLess(Expression left, Expression right) {
    if (left instanceof Less || left instanceof LessOrEqual) {
      return left;
    }

    if (right instanceof Less || right instanceof LessOrEqual) {
      return right;
    }

    return null;
  }

  private static Expression getGreater(Expression left, Expression right) {
    if (left instanceof Greater || left instanceof GreaterOrEqual) {
      return left;
    }

    if (right instanceof Greater || right instanceof GreaterOrEqual) {
      return right;
    }

    return null;
  }

  public static String convertToNpmImport(String gitRepo, String version) {
    if (isDevelopmentVersion(version)) {

      if (gitRepo == null) {
        version = removeDevSuffix(version);
      } else {
        version = gitRepo;
        return version;
      }
    }

    Expression expression = parseVersion(version);

    String npmVersion = convertToNpmExpression(expression);

    if (npmVersion == null) {
      throw new KurentoModuleCreatorException("Version '" + version + "' in import not supported");
    }

    return npmVersion;
  }

  private static Expression parseVersion(String version) {
    Parser<Expression> parser = ExpressionParser.newInstance();
    Expression expression = parser.parse(processCaretRanges(version));
    return expression;
  }

  private static String processCaretRanges(String version) {

    if (version.startsWith("^")) {
      String plainVersion = version.substring(1);
      return ">=" + plainVersion + " & <" + (Version.valueOf(plainVersion).getMajorVersion() + 1)
          + ".0.0";
    }

    return version;
  }

  private static String convertToNpmExpression(Expression expression) {

    if (expression instanceof Equal) {
      return ((Equal) expression).getParsedVersion().toString();
    } else if (expression instanceof Less) {
      return "<" + ((Less) expression).getParsedVersion();
    } else if (expression instanceof LessOrEqual) {
      return "<=" + ((LessOrEqual) expression).getParsedVersion();
    } else if (expression instanceof Greater) {
      return ">" + ((Greater) expression).getParsedVersion();
    } else if (expression instanceof GreaterOrEqual) {
      return ">=" + ((GreaterOrEqual) expression).getParsedVersion();

    } else if (expression instanceof And) {

      And and = (And) expression;
      String left = convertToNpmExpression(and.getLeft());
      String right = convertToNpmExpression(and.getRight());

      if (left != null && right != null) {
        return left + " " + right;
      }

      return null;

    } else if (expression instanceof Or) {

      Or or = (Or) expression;
      String left = convertToNpmExpression(or.getLeft());
      String right = convertToNpmExpression(or.getRight());

      if (left != null && right != null) {
        return left + " || " + right;
      }

      return null;
    }

    return null;
  }

  public static String convertToMaven(String version) {
    if (isDevelopmentVersion(version)) {
      version = removeDevSuffix(version) + "-SNAPSHOT";
    }
    return version;
  }

  public static String convertToNpm(String gitRepo, String version) {
    if (isDevelopmentVersion(version)) {

      if (gitRepo == null) {
        version = removeDevSuffix(version);
      } else {
        version = gitRepo;
      }
    }
    return version;
  }

  private static String removeDevSuffix(String version) {
    return version.substring(0, version.length() - DEV_SUFFIX_LENGTH);
  }

  private static boolean isDevelopmentVersion(String version) {
    return !isReleaseVersion(version);
  }

  public static boolean isReleaseVersion(String version) {
    return !version.endsWith(DEV_SUFFIX);
  }

  /**
   * Compares two version strings.
   *
   * <p>
   * Use this instead of String.compareTo() for a non-lexicographical comparison that works for
   * version strings. e.g. "1.10".compareTo("1.6").
   * </p>
   *
   * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
   *
   * @param str1
   *          a string of ordinal numbers separated by decimal points.
   * @param str2
   *          a string of ordinal numbers separated by decimal points.
   * @return The result is a negative integer if str1 is _numerically_ less than str2. The result is
   *         a positive integer if str1 is _numerically_ greater than str2. The result is zero if
   *         the strings are _numerically_ equal.
   */
  public static Integer versionCompare(String str1, String str2) {

    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");

    int idx = 0;
    // set index to first non-equal ordinal or length of shortest version
    // string
    while (idx < vals1.length && idx < vals2.length && vals1[idx].equals(vals2[idx])) {
      idx++;
    }
    // compare first non-equal ordinal number
    if (idx < vals1.length && idx < vals2.length) {
      int diff = Integer.valueOf(vals1[idx]).compareTo(Integer.valueOf(vals2[idx]));
      return Integer.signum(diff);
    } else {
      // the strings are equal or one string is a substring of the other
      // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
      return Integer.signum(vals1.length - vals2.length);
    }
  }

  public static boolean devCompatibleVersion(String importVersion, String depVersion) {

    if (importVersion.equals(depVersion)) {
      return true;
    }

    if (isDevelopmentVersion(importVersion)) {
      importVersion = removeDevSuffix(importVersion);
      importVersion = "^" + importVersion;
    }

    if (isDevelopmentVersion(depVersion)) {
      depVersion = removeDevSuffix(depVersion);
    }

    return compatibleVersion(importVersion, depVersion);
  }

  public static boolean compatibleVersion(String importVersion, String depVersion) {

    if (importVersion.equals(depVersion)) {
      return true;
    }

    if (isDevelopmentVersion(importVersion)) {
      return false;

    } else {
      Expression expression = parseVersion(importVersion);

      return expression.interpret(Version.valueOf(depVersion));
    }
  }
}
