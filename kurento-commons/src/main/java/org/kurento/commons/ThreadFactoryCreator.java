package org.kurento.commons;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadFactoryCreator {

  private static final AtomicLong numExecutor = new AtomicLong(0);

  public static ThreadFactory create(String name) {
    return new ThreadFactoryBuilder()
        .setNameFormat(name + "-e" + numExecutor.incrementAndGet() + "-t%d").build();
  }

}
