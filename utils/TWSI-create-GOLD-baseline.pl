#!/usr/bin/perl
#

if ($#ARGV < 1) {
	print "usage: perl TWSI-create-GOLD-baseline.pl TWSI-contexts-file TWSI-sense-inventory-file \n";
	exit(1);
}
$input = $ARGV[0];

$inventory = $ARGV[1];


# usage: 	perl TWSI-create-GOLD-baseline.pl TWSI-contexts TWSI-sense-inventory 
# e.g:		perl ./utils/TWSI-create-GOLD-baseline.pl ./data/TWSI-2.0-all-contexts.txt ./data/Inventory-TWSI-2.csv


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
open FILE, "$input" or die "Can't open file $input: $!\n";

print "#context_id\ttarget\ttarget_pos\ttarget_position\tgold_sense_ids\tpredict_sense_ids\tgolden_related\tpredict_related\tcontext\n";

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
	                                            
	@id = split(/\@\@/, $line[0]);
	print "$line[3]\t$line[1]\tn\t$start,$end\t$id[1]\t$id[1]\t$related_words{$line[0]}\t$related_words{$line[0]}\t$context\n";

}
close FILE;


