package org.w3.readwriteweb

import unfiltered.response._

class MSAuthorVia(value: String) extends ResponseHeader("MS-Author-Via", List(value))

object ViaSPARQL extends MSAuthorVia("SPARQL")
