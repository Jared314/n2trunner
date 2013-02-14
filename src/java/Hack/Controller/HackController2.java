package Hack.Controller;

import java.io.File;

// Hack
public class HackController2 extends HackController
{
  public HackController2(HackSimulator simulator, String scriptFileName) {
    super(simulator, scriptFileName);
  }

  // Hack: Overloaded to prevent changing the working directory, because 
  //       the tst files and hdl files are now in seperate directories.
  protected void saveWorkingDir(File file) {}
}
