/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python;

import com.intellij.openapi.util.TextRange;
import com.jetbrains.python.inspections.PyStringFormatParser;
import junit.framework.TestCase;

import java.util.List;

import static com.jetbrains.python.inspections.PyStringFormatParser.*;

/**
 * @author yole
 */
public class PyStringFormatParserTest extends TestCase {
  public void testSimple() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("abc").parse();
    assertEquals(1, chunks.size());
    assertConstant(chunks.get(0), 0, 3);
  }

  private static void assertConstant(FormatStringChunk aChunk, final int start, final int end) {
    ConstantChunk chunk = (ConstantChunk) aChunk;
    assertEquals(start, chunk.getStartIndex());
    assertEquals(end, chunk.getEndIndex());
  }

  public void testDoublePercent() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("abc%%def").parse();
    assertEquals(1, chunks.size());
    assertConstant(chunks.get(0), 0, 8);
  }

  public void testFormat() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%s").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals(0, chunk.getStartIndex());
    assertEquals(2, chunk.getEndIndex());
    assertEquals('s', chunk.getConversionType());
  }

  public void testSubstitutionAfterFormat() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("Hello, %s").parse();
    assertEquals(2, chunks.size());
    assertConstant(chunks.get(0), 0, 7);
  }

  public void testMappingKey() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%(language)s").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals("language", chunk.getMappingKey());
    assertEquals('s', chunk.getConversionType());
  }

  public void testConversionFlags() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%#0d").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals("#0", chunk.getConversionFlags());
  }

  public void testWidth() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%345d").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals("345", chunk.getWidth());
  }

  public void testPrecision() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%.2d").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals("2", chunk.getPrecision());
  }

  public void testLengthModifier() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%ld").parse();
    assertEquals(1, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals('l', chunk.getLengthModifier());
  }

  public void testDoubleAsterisk() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%**d").parse();
    assertEquals(2, chunks.size());
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals(2, chunk.getEndIndex());
    assertEquals('\0', chunk.getConversionType());
  }

  public void testUnclosedMapping() {
    List<PyStringFormatParser.FormatStringChunk> chunks = new PyStringFormatParser("%(name1s").parse();
    SubstitutionChunk chunk = (SubstitutionChunk) chunks.get(0);
    assertEquals("name1s", chunk.getMappingKey());
    assertTrue(chunk.isUnclosedMapping());
  }

  // PY-8372
  public void testNewStyleAutomaticNumbering() {
    final List<SubstitutionChunk> chunks = filterSubstitutions(parseNewStyleFormat("{}, {}"));
    assertEquals(2, chunks.size());
    assertEquals(TextRange.create(0, 2), chunks.get(0).getTextRange());
    assertEquals(TextRange.create(4, 6), chunks.get(1).getTextRange());
  }

  // PY-8372
  public void testNewStylePositionalArgs() {
    final List<SubstitutionChunk> chunks = filterSubstitutions(parseNewStyleFormat("{1}, {0}"));
    assertEquals(2, chunks.size());
    assertEquals(TextRange.create(0, 3), chunks.get(0).getTextRange());
    assertEquals(TextRange.create(5, 8), chunks.get(1).getTextRange());
  }

  // PY-8372
  public void testNewStyleKeywordArgs() {
    final List<SubstitutionChunk> chunks = filterSubstitutions(parseNewStyleFormat("a{foo}{bar}bc"));
    assertEquals(2, chunks.size());
    assertEquals(TextRange.create(1, 6), chunks.get(0).getTextRange());
    assertEquals(TextRange.create(6, 11), chunks.get(1).getTextRange());
  }

  // PY-8372
  public void testBracesEscaping() {
    final List<SubstitutionChunk> chunks = filterSubstitutions(parseNewStyleFormat("\\{\\}, {{}}"));
    assertEquals(1, chunks.size());
    assertEquals(TextRange.create(1, 4), chunks.get(0).getTextRange());
  }
}
