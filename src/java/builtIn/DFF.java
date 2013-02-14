package builtIn;

import Hack.Gates.BuiltInGate;
import Hack.Gates.Node;

public class DFF extends BuiltInGate
{
  private short state;

  protected void clockUp()
  {
    this.state = this.inputPins[0].get();
  }

  protected void clockDown()
  {
    this.outputPins[0].set(this.state);
  }
}