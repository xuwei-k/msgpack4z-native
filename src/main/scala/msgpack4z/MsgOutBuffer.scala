package msgpack4z

import java.io.{DataOutputStream, ByteArrayOutputStream}
import java.math.BigInteger

final class MsgOutBuffer private(arrayBuffer: ByteArrayOutputStream, buf: DataOutputStream) extends MsgPacker{

  override def result: Array[Byte] = {
    buf.close()
    arrayBuffer.toByteArray
  }

  private[msgpack4z] def writeByteAndShort(b: Byte, sh: Short): Unit = {
    buf.writeByte(b)
    buf.writeShort(sh)
  }

  private[this] def writeByteAndByte(b: Byte, b1: Byte): Unit = {
    buf.writeByte(b)
    buf.writeByte(b1)
  }

  private[msgpack4z] def writeByteAndInt(b: Byte, i: Int): Unit = {
    buf.writeByte(b)
    buf.writeInt(i)
  }

  private[msgpack4z] def writeByteAndLong(b: Byte, l: Long): Unit = {
    buf.writeByte(b)
    buf.writeLong(l)
  }

  private[this] def writeByteAndFloat(b: Byte, f: Float): Unit = {
    buf.writeByte(b)
    buf.writeFloat(f)
  }

  private[this] def writeByteAndDouble(b: Byte, d: Double) = {
    buf.writeByte(b)
    buf.writeDouble(d)
  }

  override def packByte(b: Byte): Unit = {
    if (b < -(1 << 5)) {
      buf.writeByte(Code.INT8)
      buf.writeByte(b)
    } else {
      buf.writeByte(b)
    }
  }

  def packShort(v: Short): Unit = {
    if (v < -(1 << 5)) {
      if (v < -(1 << 7)) {
        writeByteAndShort(Code.INT16, v)
      } else {
        writeByteAndByte(Code.INT8, v.asInstanceOf[Byte])
      }
    } else if (v < (1 << 7)) {
      buf.writeByte(v.asInstanceOf[Byte])
    } else {
      if (v < (1 << 8)) {
        writeByteAndByte(Code.UINT8, v.asInstanceOf[Byte])
      }
      else {
        writeByteAndShort(Code.UINT16, v)
      }
    }
  }

  def packInt(r: Int): Unit = {
    if (r < -(1 << 5)) {
      if (r < -(1 << 15)) {
        writeByteAndInt(Code.INT32, r)
      } else if (r < -(1 << 7)) {
        writeByteAndShort(Code.INT16, r.asInstanceOf[Short])
      } else {
        writeByteAndByte(Code.INT8, r.asInstanceOf[Byte])
      }
    } else if (r < (1 << 7)) {
      buf.writeByte(r.asInstanceOf[Byte])
    } else {
      if (r < (1 << 8)) {
        writeByteAndByte(Code.UINT8, r.asInstanceOf[Byte])
      } else if (r < (1 << 16)) {
        writeByteAndShort(Code.UINT16, r.asInstanceOf[Short])
      } else {
        writeByteAndInt(Code.UINT32, r)
      }
    }
  }


  def packLong(v: Long): Unit = {
    if (v < -(1L << 5)) {
      if (v < -(1L << 15)) {
        if (v < -(1L << 31)) {
          writeByteAndLong(Code.INT64, v)
        } else {
          writeByteAndInt(Code.INT32, v.asInstanceOf[Int])
        }
      } else {
        if (v < -(1 << 7)) {
          writeByteAndShort(Code.INT16, v.asInstanceOf[Short])
        } else {
          writeByteAndByte(Code.INT8, v.asInstanceOf[Byte])
        }
      }
    } else if (v < (1 << 7)) {
      buf.writeByte(v.asInstanceOf[Byte])
    } else {
      if (v < (1L << 16)) {
        if (v < (1 << 8)) {
          writeByteAndByte(Code.UINT8, v.asInstanceOf[Byte])
        } else {
          writeByteAndShort(Code.UINT16, v.asInstanceOf[Short])
        }
      } else {
        if (v < (1L << 32)) {
          writeByteAndInt(Code.UINT32, v.asInstanceOf[Int])
        } else {
          writeByteAndLong(Code.UINT64, v)
        }
      }
    }
  }

  def packBigInteger(bi: BigInteger): Unit = {
    if (bi.bitLength() <= 63) {
      packLong(bi.longValue())
    } else if (bi.bitLength() == 64 && bi.signum() == 1) {
      writeByteAndLong(Code.UINT64, bi.longValue())
    } else {
      throw new IllegalArgumentException("Messagepack cannot serialize BigInteger larger than 2^64-1")
    }
  }

  def packFloat(v: Float): Unit = {
    writeByteAndFloat(Code.FLOAT32, v)
  }

  def packDouble(v: Double): Unit = {
    writeByteAndDouble(Code.FLOAT64, v)
  }

  def close(): Unit = {
    buf.close()
  }

  override def packArrayHeader(size: Int): Unit = {
    if(0 <= size) {
      if(size < (1 << 4)) {
        buf.writeByte((Code.FIXARRAY_PREFIX | size).asInstanceOf[Byte])
      } else if(size < (1 << 16)) {
        writeByteAndShort(Code.ARRAY16, size.asInstanceOf[Short])
      } else {
        writeByteAndInt(Code.ARRAY32, size)
      }
    } else {
      writeByteAndInt(Code.ARRAY32, size)
    }
  }

  override def packBinary(array: Array[Byte]): Unit = {
    val len = array.length
    if(len < (1 << 8)) {
      writeByteAndByte(Code.BIN8, len.asInstanceOf[Byte])
    } else if(len < (1 << 16)) {
      writeByteAndShort(Code.BIN16, len.asInstanceOf[Short])
    } else {
      writeByteAndInt(Code.BIN32, len)
    }
    buf.write(array)
  }

  override def packNil(): Unit = {
    buf.writeByte(Code.NIL)
  }

  override def mapEnd(): Unit = {
    // do nothing
  }

  override def packMapHeader(size: Int): Unit = {
    if(0 <= size) {
      if (size < (1 << 4)) {
        buf.writeByte((Code.FIXMAP_PREFIX | size).asInstanceOf[Byte])
      } else if (size < (1 << 16)) {
        writeByteAndShort(Code.MAP16, size.asInstanceOf[Short])
      } else {
        writeByteAndInt(Code.MAP32, size)
      }
    } else {
      writeByteAndInt(Code.MAP32, size)
    }
  }

  override def packBoolean(a: Boolean): Unit = {
    buf.writeByte(if(a) Code.TRUE else Code.FALSE)
  }

  private[this] def writeStringHeader(len: Int): Unit = {
    if(len < (1 << 5)) {
      buf.writeByte((Code.FIXSTR_PREFIX | len).asInstanceOf[Byte])
    } else if(len < (1 << 8)) {
      writeByteAndByte(Code.STR8, len.asInstanceOf[Byte])
    } else if(len < (1 << 16)) {
      writeByteAndShort(Code.STR16, len.asInstanceOf[Short])
    } else {
      writeByteAndInt(Code.STR32, len)
    }
  }

  override def packString(str: String): Unit = {
    val bytes = str.getBytes("UTF-8")
    writeStringHeader(bytes.length)
    buf.write(bytes)
  }

  override def arrayEnd(): Unit = {
    // do nothing
  }
}

object MsgOutBuffer {
  def create(): MsgOutBuffer = {
    val out = new ByteArrayOutputStream()
    val data = new DataOutputStream(out)
    new MsgOutBuffer(out, data)
  }
}