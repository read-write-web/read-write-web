/*
 * Copyright (c) 2012 Henry Story (bblfish.net)
 * under the MIT licence defined at
 *    http://www.opensource.org/licenses/mit-license.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.w3.readwriteweb.util

import com.weiglewilczek.slf4s.Logging
import java.lang.{Class, String}
import java.net.InetAddress
import java.io.FileDescriptor
import java.security.Permission

/**
 * This class can be set as the SecurityManager on the command line with
 * -Djava.security.manager=org.w3.readwriteweb.util.NetworkLoggingSM
 * IT will show all attempts to make outbound network connections, which can be useful to track connections made in
 * various libraries
 */
class NetworkLoggingSM extends AllowAllSecurityManager with Logging {

  override def checkConnect(host: String, port: Int) {
    logger.info("connecting to "+host+":"+port)
    super.checkConnect(host,port)
  }

  override def checkConnect(host: String, port: Int, context: AnyRef) {
    logger.info("connecting to "+host+":"+port)
    super.checkConnect(host,port,context)
  }
    
}

class AllowAllSecurityManager extends SecurityManager {
  override def checkAccept(host: String, port: Int) {}


  override def checkAwtEventQueueAccess() {}

  override def checkConnect(host: String, port: Int) {}

  override def checkConnect(host: String, port: Int, context: AnyRef) {}

  override def checkCreateClassLoader() {}

  override def checkDelete(file: String) {}

  override def checkExec(cmd: String) {}

  override def checkExit(status: Int) {}

  override def checkLink(lib: String) {}

  override def checkListen(port: Int) {}

  override def checkMemberAccess(clazz: Class[_], which: Int) {}

  override def checkMulticast(maddr: InetAddress) {}

  override def checkMulticast(maddr: InetAddress, ttl: Byte) {}

  override def checkPackageAccess(pkg: String) {}

  override def checkPackageDefinition(pkg: String) {}

  override def checkPermission(perm: Permission) {}

  override def checkPermission(perm: Permission, context: AnyRef) {}

  override def checkPrintJobAccess() {}

  override def checkPropertiesAccess() {}

  override def checkPropertyAccess(key: String) {}

  override def checkRead(fd: FileDescriptor) {}

  override def checkRead(file: String) {}

  override def checkRead(file: String, context: AnyRef) {}

  override def checkSecurityAccess(target: String) {}

  override def checkSetFactory() {}

  override def checkSystemClipboardAccess() {}

  override def checkTopLevelWindow(window: AnyRef) = false

  override def checkWrite(fd: FileDescriptor) {}

  override def checkWrite(file: String) {}
}

trait WrappedSecurityManager extends SecurityManager  {
  val wrap: SecurityManager
  override def getSecurityContext = wrap.getSecurityContext

  override def checkPermission(perm: Permission) { wrap.checkPermission(perm)}

  override def checkPermission(perm: Permission, context: AnyRef) {wrap.checkPermission(perm,context)}

  override def checkCreateClassLoader() { wrap.checkCreateClassLoader() }

  override def checkAccess(t: Thread) { wrap.checkAccess(t)}

  override def checkAccess(g: ThreadGroup) { wrap.checkAccess(g)}

  override def checkExit(status: Int) { wrap.checkExit(status) }

  override def checkExec(cmd: String) { wrap.checkExec(cmd) }

  override def checkLink(lib: String) { wrap.checkLink(lib)}

  override def checkRead(fd: FileDescriptor) {wrap.checkRead(fd)}

  override def checkRead(file: String) { wrap.checkRead(file)}

  override def checkRead(file: String, context: AnyRef) { wrap.checkRead(file)}

  override def checkWrite(fd: FileDescriptor) {wrap.checkWrite(fd)}

  override def checkWrite(file: String) {wrap.checkWrite(file)}

  override def checkDelete(file: String) {wrap.checkDelete(file)}

  override def checkListen(port: Int) {wrap.checkListen(port)}

  override def checkAccept(host: String, port: Int) {wrap.checkAccept(host,port)}

  override def checkMulticast(maddr: InetAddress) { wrap.checkMulticast(maddr)}

  override def checkPropertiesAccess() { wrap.checkPropertiesAccess()}

  override def checkPropertyAccess(key: String) {wrap.checkPropertyAccess(key)}

  override def checkTopLevelWindow(window: AnyRef) = wrap.checkTopLevelWindow(window)

  override def checkPrintJobAccess() { wrap.checkPrintJobAccess()}

  override def checkSystemClipboardAccess() { wrap.checkSystemClipboardAccess()}

  override def checkAwtEventQueueAccess() { wrap.checkAwtEventQueueAccess()}

  override def checkPackageAccess(pkg: String) { wrap.checkPackageAccess(pkg)}

  override def checkPackageDefinition(pkg: String) {wrap.checkPackageDefinition(pkg)}

  override def checkSetFactory() {wrap.checkSetFactory()}

  override def checkMemberAccess(clazz: Class[_], which: Int) {wrap.checkMemberAccess(clazz,which)}

  override def checkSecurityAccess(target: String) {wrap.checkSecurityAccess(target)}

  override def getThreadGroup = wrap.getThreadGroup
}






