#!/usr/bin/perl
#

if ($#ARGV < 2) {
	print "usage: perl TWSI-create-GOLD-baseline.pl TWSI-contexts-file TWSI-sense-inventory-file TWSI-most-frequent-senses-file\n";
	exit(1);
}
$input = $ARGV[0];

$inventory = $ARGV[1];

$mfs_file = $ARGV[2];


# usage: 	perl TWSI-create-MFS-baseline.pl TWSI-contexts TWSI-sense-inventory TWSI-most-frequent-senses
# e.g:		perl ./utils/TWSI-create-MFS-baseline.pl ./data/TWSI-2.0-all-contexts.txt ./data/Inventory-TWSI-2.csv ./data/TWSI-most-frequent-senses.txt


# file read
open SENSE, "$inventory" or die "Can't open file $inventory: $!\n";


# store TWSI senses
while (<SENSE>) {  
	chomp;
	@line = split(/\t/,$_);
	$related_words{$line[0]."@@".$line[1]} = $line[2];
}
close SENSE;



# file read
open MFS, "$mfs_file" or die "Can't open file $mfs_file: $!\n";


# store MFS for each word
while (<MFS>) {  
	chomp;
	@line = split(/\t/,$_);
	$mfs{$line[0]} = $line[1];
}
close MFS;



# file read
open FILE, "$input" or die "Can't open file $input: $!\n";

print "context_id\ttarget\ttarget_pos\ttarget_position\tgold_sense_ids\tpredict_sense_ids\tgolden_related\tpredict_related\tcontext\n";

# convert TWSI contexts
while (<FILE>) {  
	chomp;
	@line = split(/\t/,$_);
	if ($#line != 5) {
		print $#line;
		exit(1);
	}
	$context = $line[4];
	$start = index($context, "<b>");
	$end = $start+length($line[2]);
	$context =~ s/\<\/?b\>//g;
	
	$mfs_key = $mfs{$line[1]};
	@mfs_id = split(/\@\@/, $mfs_key);
	                                            
	@id = split(/\@\@/, $line[0]);
	# check for duplicate entries
	if (exists($processedEntries{"$line[3]$line[2]"}) && $processedEntries{"$line[3]$line[2]"} == $id[1]) {
		print STDERR "duplicate entry $c: Sentence ID: $line[3]\tTarget Lemma: $line[1]\n";
		$c++;
		next;
	} else {
		print "$line[3]\t$line[1]\tn\t$start,$end\t$id[1]\t$mfs_id[1]\t$related_words{$line[0]}\t$related_words{$mfs_key}\t$context\n";
		$processedEntries{"$line[3]$line[2]"} = $id[1];
	}
}
close FILE;

