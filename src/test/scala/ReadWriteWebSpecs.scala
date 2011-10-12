package org.w3.readwriteweb

import org.specs._

object ReadWriteWebSpec extends Specification {
  "The Read Write Web".isSpecifiedBy(
      // access content
      GetStrictModeSpec, GetWikiModeSpec,
      ContentNegociationSpec,
      // create content
      PutRDFXMLSpec, PostRDFSpec,
      PutInvalidRDFXMLSpec, PostOnNonExistingResourceSpec,
      // sparql query
      PostSelectSpec, PostConstructSpec, PostAskSpec, 
      // sparql update
      PostInsertSpec,
      // delete content
      DeleteResourceSpec,
      // common errors
      PostBouleshitSpec
  )

}