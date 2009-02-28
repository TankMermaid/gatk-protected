#!/usr/bin/env python

import getopt, sys, os, string
from farm_commands import *

#FastaQuals2Fastq_exe = "/wga/dev/andrewk/Arachne/AlignerEvaluation/FastaQuals2Fastq.py"
FastaQuals2Fastq_exe = "FastaQuals2Fastq.py"
        
def isFastaB(filename):
    """Is the file a fastb file already?"""
    #print os.path.splitext(filename)
    return os.path.splitext(filename)[1] == '.fastb'

def readListOfLanes( listFile ):
    """Simply reads a list of files to process from a file"""
    lines = map( string.split, map( string.strip, open(listFile).readlines() ) )
    return map( lambda x: x[0], lines )
    #return map( lambda x: x[0], lines ), map( lambda x: x[1], lines )
    

def run_swmerlin(input_file, input_head, farm=""):
    run_merlin(input_file, input_head, farm, sw=True)

def run_merlin(input_file, input_head, farm="", sw=False):
    "sw = Merlin Smith-Waterman option"
    if isFastaB(input_file):
        input_fastb = input_file
    else:
        input_fastb = input_head+".fastb"
        if regenExistingFiles or not os.path.exists(input_fastb):
            cmd("Fasta2Fastb IN= "+input_file, just_print_commands=justPrintCommands)
    if sw:
        output_head = input_head+".swmerlin"
    else:
        output_head = input_head+".merlin"
    cmd_str = "Merlin REF_FASTB= /seq/references/Homo_sapiens_assembly18/v0/Homo_sapiens_assembly18.fasta.lookuptable.fastb REF_MERLIN= /seq/references/Homo_sapiens_assembly18/v0/Homo_sapiens_assembly18.fasta.merlinref.bin FASTB= "+input_fastb+" OUT_HEAD="+output_head
    if sw:
        cmd_str += " SW=True"
    cmd(cmd_str, farm, output_head, just_print_commands=justPrintCommands)

USE_BATCH = False

def run_ilt(input_file, input_head, farm=""):
    #print 'isFastaB', input_file, isFastaB(input_file)
    if isFastaB(input_file):
        input_fastb = input_file
    else:
        input_fastb = input_head+".fastb"
        if regenExistingFiles or not os.path.exists(input_fastb):
            cmd("Fasta2Fastb IN= "+input_file, just_print_commands=justPrintCommands)

    output_head = input_head+".ilt"

    if USE_BATCH:
        cmd_str = "~depristo/bin/batchShortQueryLookup2.pl --NUMPROCS=10 --BATCHQUEUE=long --SEQS="+input_fastb+" --L=/seq/references/Homo_sapiens_assembly18/v0/Homo_sapiens_assembly18.fasta.lookuptable.lookup --MAX_FREQ=1000 --O= "+output_head
        cmd(cmd_str, False, input_head, just_print_commands=justPrintCommands)
    else:
        cmd_str = "ImperfectLookupTable SEQS= "+input_fastb+" L= /seq/references/Homo_sapiens_assembly18/v0/Homo_sapiens_assembly18.fasta.lookuptable.lookup MAX_FREQ=1000 OUT_PREFIX= "+output_head
        cmd(cmd_str, farm, output_head, just_print_commands=justPrintCommands)

def run_BWA32(input_file, input_head, farm=""):
    run_BWA(input_file, input_head, farm, 32)

def run_BWA(fasta, input_head, farm="", seed='inf'):
    output_head = input_head+".bwa"
    quals = input_head+".quals.txt"
    fastq = input_head+".fastq"
    if regenExistingFiles or not os.path.exists(fastq) :
        cmd_str = FastaQuals2Fastq_exe+" "+fasta+" "+quals+" "+fastq
        cmd(cmd_str)
    if seed <> 'inf':
        output_head += str(seed)
    output_sam = output_head + ".sam"
    cmd_str = "bwahuman samse "+str(seed)+" "+fastq+" "+output_sam
    cmd(cmd_str, farm, output_head, just_print_commands=justPrintCommands)

def run_MAQ(input_fasta, head, farm=""):
    maq_exe = "/seq/dirseq/maq-0.7.1/maq"
    bfa_ref="/seq/dirseq/ktibbett/maq-0.7.1-test/Homo_sapiens_assembly18.bfa"

    fasta = input_fasta
    quals = head+".quals.txt"
    fastq = head+".fastq"
    if regenExistingFiles or not os.path.exists(fastq) :
        cmd_str = FastaQuals2Fastq_exe+" "+fasta+" "+quals+" "+fastq
        cmd(cmd_str)

    bfq = head+".bfq"
    if regenExistingFiles or not os.path.exists(bfq):
        cmd( maq_exe+" fastq2bfq "+fastq+" "+bfq, just_print_commands=justPrintCommands )

    out_head = head+".maq"
    maq_out = out_head+".out.aln.map"
    cmd_str = maq_exe+" map -e 100 -a 600 -s 0 "+maq_out+" "+bfa_ref+" "+bfq
    cmd(cmd_str, farm, out_head, just_print_commands=justPrintCommands)
    
def usage():
    print "Required arguments:"
    print "  -i         Input FASTA head (*.fasta, *.qualb)"
    print "       OR"
    print "  -d         Directory to grab all FASTA files from"
    print "       OR"
    print "  -l         List of FASTA/FASTB files to process"
    print
    print "Optional arguments:"
    print "  -f QUEUE   Farm jobs to QUEUE on LSF"
    print
    print "  -m MAPPER  Compare output from MAPPER which can be: ilt, merlin, swmerlin, maq, all (default: all)"
    print
    print "  -x         Don't execute commands, just print them"
    print
    print "  -w         Output files to current directory (strip path from input file/dir/list"
    print


def get_all_fasta_files(fasta_dir):
    files = os.listdir(fasta_dir)
    if not fasta_dir.endswith("/"): fasta_dir += "/"
    fasta_files = [fasta_dir+f for f in files if f.endswith(".fasta") and os.path.getsize(fasta_dir+f) > 0]
    #print fasta_files
    return fasta_files

justPrintCommands = False
regenExistingFiles = False

if __name__ == "__main__":
    opts = None
    try:
        opts, args = getopt.getopt(sys.argv[1:], "i:d:f:m:l:xw:r", ["input","fasta_dir","farm","mapper","listOfLanes", "dontexe", "regenExistingFiles", "outputInWorkingDirectory"])
    except getopt.GetoptError:
        print sys.argv
        usage()
        sys.exit(2)

    input_head = ""
    fasta_dir = ""
    mapper_str = "all"
    farm_sub = False
    listOfLanes = None
    outputInWorkingDirectory = False

    for opt, arg in opts:
        print opt, arg
        if opt in ("-i", "--input"):
            input_head = arg
        if opt in ("-l", "--listOfLanes"):
            listOfLanes = arg
        if opt in ("-d", "--fasta_dir"):
            fasta_dir = arg
        if opt in ("-f", "--farm"):
            farm_sub = arg
        if opt in ("-r", "--regenExistingFiles"):
            regenExistingFiles = True
        if opt in ("-m", "--mapper"):
            mapper_str = arg
        if opt in ("-x", "--dontexe"):
            justPrintCommands = True
        if opt in ("-w", "--outputInWorkingDirectory"):
            outputInWorkingDirectory = True

    if (input_head == "") and (fasta_dir == "") and (listOfLanes == None):
        print input_head, fasta_dir, listOfLanes
        usage()
        sys.exit(2)

    # Select function(s) for mapper
    mapper_func_list = {"ilt":run_ilt, "merlin":run_merlin, "swmerlin":run_swmerlin, "maq":run_MAQ, "bwa":run_BWA, "bwa32":run_BWA32}
    if mapper_str.lower() == "all":
        mapper_list = mapper_func_list.values()
    else:
        mapper_list = [mapper_func_list.get(mapper_str.lower())]
        if mapper_list == [None]:
            sys.exit("Don't know of mapper argument: "+mapper_str)

    if input_head:
        input_heads = [None]
        input_files = [input_head + 'fasta']
    elif listOfLanes <> None:
        input_files = readListOfLanes(listOfLanes)
        input_heads = [None] * len(input_files)
    else:
        input_files = [file for file in get_all_fasta_files(fasta_dir)]
        input_heads = [None] * len(input_files)
        
    for input_file, input_head in zip(input_files, input_heads):
        if input_head == None:
            file_head = os.path.splitext(input_file)[0]
            if outputInWorkingDirectory:
                file_head = os.path.split(file_head)[1]
        else:
            file_head = input_head
        for mapper in mapper_list:
            mapper( input_file, file_head, farm=farm_sub )
            print
