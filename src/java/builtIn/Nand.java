package builtIn;

import Hack.Gates.BuiltInGate;
import Hack.Gates.Node;

public class Nand extends BuiltInGate
{
  protected void reCompute()
  {
    int i = this.inputPins[0].get();
    int j = this.inputPins[1].get();
    this.outputPins[0].set((short)(1 - (i & j)));
  }
}