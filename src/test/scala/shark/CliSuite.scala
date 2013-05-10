/*
 * Copyright (C) 2012 The Regents of The University California.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shark

import java.io.{BufferedReader, File, InputStreamReader, PrintWriter}
import org.scalatest.{BeforeAndAfterAll, FunSuite}


/**
 * Test the Shark CLI.
 */
class CliSuite extends FunSuite with BeforeAndAfterAll with CliTestToolkit {

  val WAREHOUSE_PATH = CliTestToolkit.getWarehousePath("cli")
  val METASTORE_PATH = CliTestToolkit.getMetastorePath("cli")

  override def beforeAll() {
    val pb = new ProcessBuilder(
      "./bin/shark",
      "-hiveconf",
      "javax.jdo.option.ConnectionURL=jdbc:derby:;databaseName=" + METASTORE_PATH + ";create=true",
      "-hiveconf",
      "hive.metastore.warehouse.dir=" + WAREHOUSE_PATH)

    process = pb.start()
    outputWriter = new PrintWriter(process.getOutputStream, true)
    inputReader = new BufferedReader(new InputStreamReader(process.getInputStream))
    errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream))
    waitForOutput(inputReader, "shark>")
  }

  override def afterAll() {
    process.destroy()
    process.waitFor()
  }

  test("simple select") {
    val dataFilePath = CliTestToolkit.dataFilePath + "/kv1.txt"
    executeQuery("create table shark_test1(key int, val string);")
    executeQuery("load data local inpath '" + dataFilePath+ "' overwrite into table shark_test1;")
    executeQuery("""create table shark_test1_cached TBLPROPERTIES ("shark.cache" = "true") as
      select * from shark_test1;""")
    val out = executeQuery("select * from shark_test1_cached where key = 407;")
    assert(out.contains("val_407"))
    assert(isCachedTable("shark_test1_cached"))
  }

  def isCachedTable(tableName: String) : Boolean = {
    val dir = new File(WAREHOUSE_PATH + "/" + tableName.toLowerCase)
    dir.isDirectory && dir.listFiles.isEmpty
  }
}