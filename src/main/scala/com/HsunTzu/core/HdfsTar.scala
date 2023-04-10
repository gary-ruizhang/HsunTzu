package com.HsunTzu.core

import java.io.BufferedInputStream
import com.HsunTzu.utils.CommonUtils
import com.typesafe.scalalogging.Logger
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, FileStatus, FileSystem, Path}
import org.apache.hadoop.io.IOUtils
import org.apache.hadoop.io.compress._
import org.slf4j.LoggerFactory

class HdfsTar {

}

object  HdfsTar{

  private[this] val logger =Logger(LoggerFactory.getLogger(classOf[HdfsTar]))
  /***
    * 对 hdfs  原始文件目录  进行 tar ball  压缩 默认 使用gzip
    * @param fs  hdfs 文件系统上下文
    * @param conf  hdfs 文件的配置 上下文
    * @param inpath   hdfs 文件输入目录
    * @param outPath hdfs  tarball 输出目录
    * @param codec    hdfs tar 压缩格式 方法
    * @param depth   压缩目录的深度 默认为1 层 目录
    */
  def makeTarArchiveForDir(fs: FileSystem, conf: Configuration, inpath: String, outPath: String,codec:String, size: Int)(depth:Int=1):Unit={
    val inputPath:Path=new Path(inpath)

    val inSubPath: String =CommonUtils.getOutFileSubPath(inpath)
    var nOutPath = ""
    if (outPath.endsWith("/")) {
      nOutPath = outPath.substring(0, outPath.length - 1)
    } else {
      nOutPath = outPath
    }
    val gzipCodec:GzipCodec=new GzipCodec()
    gzipCodec.setConf(conf)
    val tarFile=outPath + ".tar" + gzipCodec.getDefaultExtension
    //val bos:ByteArrayInputStream=new ByteArrayInputStream()
    val outputPath:Path=new Path(tarFile)
    val  outFsData:FSDataOutputStream=fs.create(outputPath)
    val compressOutStream:CompressionOutputStream=gzipCodec.createOutputStream(outFsData)
    val tarOutStream:TarArchiveOutputStream=new TarArchiveOutputStream(compressOutStream)
    // val tarOutStream:TarOutputStream=new TarOutputStream(compressOutStream)
    var readlen=0
    val bufferIO:Array[Byte]=new Array[Byte](64*1024)
    val startTime=System.currentTimeMillis()

    val listFs: Array[FileStatus] = fs.listStatus(inputPath)
    var i = 0;
    var j = 0;
    while (i < listFs.length && j < size) {
      val file = listFs(i)
      val inFsData: FSDataInputStream = fs.open(file.getPath)
      val buffInStream: BufferedInputStream = new BufferedInputStream(inFsData)

      val entry = new TarArchiveEntry(file.getPath.getName)
      entry.setSize(file.getLen)
      tarOutStream.putArchiveEntry(entry)

      try {
        while ( {
          readlen = buffInStream.read(bufferIO)
          readlen != -1
        }) {
          tarOutStream.write(bufferIO, 0, readlen)
        }
        tarOutStream.flush()
        tarOutStream.closeArchiveEntry()
      } catch {
        case e: Exception => e.printStackTrace()
      } finally {
        IOUtils.closeStream(inFsData)
        fs.delete(file.getPath, true)
        // fs.close()
      }
      i = i + 1
      j = j + 1
    }

    tarOutStream.finish()
    tarOutStream.close()
    IOUtils.closeStream(outFsData)

    val endTime=System.currentTimeMillis()
    val tmCause=endTime-startTime
    logger.info(" 时间消耗 ： "+tmCause +" ms")
  }


}

