package org.w3.readwriteweb

sealed trait RWWMode

case object AllResourcesAlreadyExist extends RWWMode

case object ResourcesDontExistByDefault extends RWWMode
