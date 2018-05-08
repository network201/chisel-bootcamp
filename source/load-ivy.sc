import $ivy.`edu.berkeley.cs::chisel3:3.1.0` 
import $ivy.`edu.berkeley.cs::chisel-iotesters:1.2.0`
import $ivy.`edu.berkeley.cs::dsptools:1.1.0`
import $ivy.`org.scalanlp::breeze:0.13.2`



// Convenience function to invoke Chisel and grab emitted Verilog.
def getVerilog(dut: => chisel3.core.UserModule): String = {
  import firrtl._
  return chisel3.Driver.execute(Array[String](), {() => dut}) match {
    case s:chisel3.ChiselExecutionSuccess => s.firrtlResultOption match {
      case Some(f:FirrtlExecutionSuccess) => f.emitted
    }
  }
}

// Convenience function to invoke Chisel and grab emitted FIRRTL.
def getFirrtl(dut: => chisel3.core.UserModule): String = {
  return chisel3.Driver.emit({() => dut})
}

def compileFirrtl(
    inputFirrtl: String,
    compiler: firrtl.Compiler,
    customTransforms: Seq[firrtl.Transform] = Seq.empty,
    infoMode: firrtl.Parser.InfoMode = firrtl.Parser.IgnoreInfo,
    annotations: firrtl.AnnotationSeq = firrtl.AnnotationSeq(Seq.empty)
): String = {
  import firrtl.{Compiler, AnnotationSeq, CircuitState, ChirrtlForm, FIRRTLException}
  import firrtl.Parser._
  import scala.io.Source
  import scala.util.control.ControlThrowable
  import firrtl.passes._
  val outputBuffer = new java.io.CharArrayWriter
  try {
      //val parsedInput = firrtl.Parser.parse(Source.fromFile(input).getLines(), infoMode)
      val parsedInput = firrtl.Parser.parse(inputFirrtl.split("\n").toIterator, infoMode)
      compiler.compile(
         CircuitState(parsedInput, ChirrtlForm, annotations),
         outputBuffer,
         customTransforms)
  }

  catch {
    // Rethrow the exceptions which are expected or due to the runtime environment (out of memory, stack overflow)
    case p: ControlThrowable => throw p
    case p: PassException  => throw p
    case p: FIRRTLException => throw p
     // Treat remaining exceptions as internal errors.
       case e: Exception => firrtl.Utils.throwInternalError(exception = Some(e))
  }

  val outputString = outputBuffer.toString
  outputString
}
