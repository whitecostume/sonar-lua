/*
 * SonarQube Lua Plugin
 * Copyright (C) 2013-2016-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.lua.lexer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;


import org.sonar.lua.api.LuaTokenType;
import org.sonar.sslr.channel.Channel;
import org.sonar.sslr.channel.CodeReader;

import java.util.List;
import java.util.Set;

import static com.sonar.sslr.impl.channel.RegexpChannelBuilder.regexp;

/**
 * Provides a heuristic to guess whether a forward slash starts a regular expression.
 * 
 */
public class LuaRegularExpressionLiteralChannel extends Channel<Lexer> {

  private final Channel<Lexer> delegate;

  private static final Set<String> WHOLE_TOKENS = ImmutableSet.of(
		 
		   
    "break",
    "do",
    "else",
    "elseif",
 
    "in",
   
    "return",


  

    // Binary operators which cannot be followed by a division operator:
    // Match + but not ++. += is handled below.
    "+",
    // Match - but not --. -= is handled below.
    "-",
    // Match . but not a number with a trailing decimal.
    ".",
    // Match /, but not a regexp. /= is handled below.
    "/",
    // Second binary operand cannot start a division.
    ",",
    // Ditto binary operand.
    "*");

  private static final String[] ENDS = new String[] {
    // ! prefix operator operand cannot start with a division
    "!",
    // % second binary operand cannot start with a division
    "%",
    // &, && ditto binary operand
    "&",
    // ( expression cannot start with a division
    "(",
    // : property value, labelled statement, and operand of ?: cannot start with a division
    ":",
    // ; statement & for condition cannot start with division
    ";",
   
    "=",
    // >, >>, >>> ditto binary operand
    ">",
    // ? expression in ?: cannot start with a division operator
    "?",
    // [ first array value & key expression cannot start with a division
    "[",
    // ^ ditto binary operand
    "^",
    // { statement in block and object property key cannot start with a division
    "{",
    // |, || ditto binary operand
    "|",
    // } PROBLEMATIC: could be an object literal divided or a block.
    // More likely to be start of a statement after a block which cannot start with a /.
    "}",
    // ~ ditto binary operand
    "~"
  };

  public LuaRegularExpressionLiteralChannel() {
    this.delegate = regexp(LuaTokenType.REGULAR_EXPRESSION_LITERAL, "/([^/\\n\\\\]*+(\\\\.)?+)*+/\\p{javaJavaIdentifierPart}*+");
  }

  @Override
  public boolean consume(CodeReader code, Lexer output) {
    if (code.peek() == '/') {
      Token lastToken = getLastToken(output);
      if (lastToken == null || guessNextIsRegexp(lastToken.getValue())) {
        return delegate.consume(code, output);
      }
    }
    return false;
  }

  private static Token getLastToken(Lexer output) {
    List<Token> tokens = output.getTokens();
    return tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
  }

  
  
  @VisibleForTesting
  static boolean guessNextIsRegexp(String preceder) {
    if (WHOLE_TOKENS.contains(preceder)) {
      return true;
    }
    for (String end : ENDS) {
      if (preceder.endsWith(end)) {
        return true;
      }
    }
    return false;
  }

}
