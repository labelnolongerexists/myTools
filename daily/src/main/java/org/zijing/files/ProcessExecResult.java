package org.zijing.files;

import java.util.List;

public class ProcessExecResult {
  public final int execCode;

  public final List<String> stdout;
  public final List<String> stderr;

  public ProcessExecResult(int execCode, List<String> stdout, List<String> stderr) {
    this.execCode = execCode;
    this.stdout = stdout;
    this.stderr = stderr;
  }
}
