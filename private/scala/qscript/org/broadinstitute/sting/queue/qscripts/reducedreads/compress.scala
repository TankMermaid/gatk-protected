package org.broadinstitute.sting.queue.qscripts.reducedreads


import org.broadinstitute.sting.queue.QScript
import org.broadinstitute.sting.queue.extensions.gatk._
import io.Source._
import org.broadinstitute.sting.utils.exceptions.UserException

/**
 * Created by IntelliJ IDEA.
 * User: carneiro
 * Date: 9/28/11
 * Time: 4:57 PM
 */


class Compress extends QScript {
  @Argument(shortName = "ref",    fullName = "reference", doc = "Reference sequence", required=true) protected val reference: File = null
  @Argument(shortName = "bam",    fullName = "bam_file", doc = "", required=false) protected val bam: File = null
  @Argument(shortName = "ls",     fullName = "list", doc ="", required=false) protected val list: File = null
  @Argument(shortName = "int",    fullName = "intervals", doc = "", required=false) protected val intervals: File = null
  @Argument(shortName = "sg",     fullName = "scatterCount", doc ="", required = false) protected val scatterCount = 50
  @Argument(shortName = "cs",     fullName = "context_size", doc = "", required = false) protected var contextSize: Option[Int] = None
  @Argument(shortName = "minmap", fullName = "minimum_mapping_quality", doc = "", required = false) protected var minMappingQuality: Option[Int] = None
  @Argument(shortName = "mintail",fullName = "minimum_tail_qualities", doc = "", required = false) protected var minTailQuality: Option[Byte] = None
  @Argument(shortName = "minvar", fullName = "minimum_alt_proportion_to_trigger_variant", doc = "", required = false) protected var minAltProportionToTriggerVariant: Option[Double] = None
  @Argument(shortName = "mindel", fullName = "minimum_del_proportion_to_trigger_variant", doc = "", required = false) protected var minIndelProportionToTriggerVariant: Option[Double] = None
  @Argument(shortName = "minqual",fullName = "minimum_base_quality_to_consider", doc = "", required = false) protected var minBaseQual: Option[Int] = None
  @Argument(shortName = "dl",     fullName = "", doc = "", required = false) protected var debugLevel: Option[Int] = None
  @Argument(shortName = "dr",     fullName = "", doc = "", required = false) protected var debugRead: String = ""
  @Argument(shortName = "ds",     fullName = "downsample_coverage", doc = "", required = false) protected var downsampleCoverage: Option[Int] = None

    trait UNIVERSAL_GATK_ARGS extends CommandLineGATK {
    this.logging_level = "INFO";
    this.reference_sequence = reference;
    this.memoryLimit = 4
  }

  def script = {

    var bamList: List[File] = List()

    if (bam != null)
      bamList :+= bam
    else if (list != null)
      for (file: String <- fromFile(list).getLines())
        bamList :+= new File(file)
    else
      throw new UserException("You have to provide either a BAM or a LIST of bams.")



    for (file <- bamList) {
      val reducedBAM = swapExt(file.getName, ".bam", ".reduced.bam")

      // reduce
      val rr = new ReduceReads() with UNIVERSAL_GATK_ARGS
      rr.input_file :+= file
      rr.out = reducedBAM
      rr.scatterCount = scatterCount
      if (intervals != null) rr.intervals :+= intervals
      if (contextSize != None) rr.context_size = Some(contextSize)
      if (minMappingQuality != None) rr.minimum_mapping_quality = Some(minMappingQuality)
      if (minTailQuality != None) rr.minimum_tail_qualities = Some(minTailQuality)
      if (minAltProportionToTriggerVariant != None) rr.minimum_alt_proportion_to_trigger_variant = Some(minAltProportionToTriggerVariant)
      if (minIndelProportionToTriggerVariant != None) rr.minimum_del_proportion_to_trigger_variant = Some(minIndelProportionToTriggerVariant)
      if (minBaseQual != None) rr.minimum_base_quality_to_consider = Some(minBaseQual)
      if (debugLevel != None) rr.debuglevel = Some(debugLevel)
      if (!debugRead.isEmpty) rr.debugread = debugRead
      if (downsampleCoverage != None) rr.downsample_coverage = Some(downsampleCoverage)

      add(rr)
    }
  }
}
