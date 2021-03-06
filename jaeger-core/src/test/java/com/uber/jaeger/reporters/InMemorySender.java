/*
 * Copyright (c) 2016, Uber Technologies, Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.jaeger.reporters;

import com.uber.jaeger.exceptions.SenderException;
import com.uber.jaeger.reporters.protocols.JaegerThriftSpanConverter;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.thriftjava.Span;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Sender which stores spans in memory. Appending a new span is a blocking operation unless
 * "permitted". By default Integer.MAX_VALUE "appends" are permitted.
 */
public class InMemorySender implements Sender {

  private List<Span> appended;
  private List<Span> flushed;
  private List<Span> received;
  private Semaphore semaphore = new Semaphore(Integer.MAX_VALUE);

  public InMemorySender() {
    appended = new ArrayList<Span>();
    flushed = new ArrayList<Span>();
    received = new ArrayList<Span>();
  }

  public List<Span> getAppended() {
    return new ArrayList<Span>(appended);
  }

  public List<Span> getFlushed() {
    return new ArrayList<Span>(flushed);
  }

  public List<Span> getReceived() {
    return new ArrayList<Span>(received);
  }

  @Override
  public int append(com.uber.jaeger.Span span) throws SenderException {
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    com.uber.jaeger.thriftjava.Span thriftSpan = JaegerThriftSpanConverter.convertSpan(span);
    appended.add(thriftSpan);
    received.add(thriftSpan);
    return 0;
  }

  @Override
  public int flush() throws SenderException {
    int flushedSpans = appended.size();
    flushed.addAll(appended);
    appended.clear();

    return flushedSpans;
  }

  @Override
  public int close() throws SenderException {
    return flush();
  }

  /**
   * Removes previously granted "append" permits and grants
   * a new number of permits
   *
   * @param number number of "appends" to permit
   */
  public void permitAppend(int number) {
    semaphore.drainPermits();
    semaphore.release(number);
  }
}
