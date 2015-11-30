#!/usr/bin/perl
#
use File::Copy qw(move);

$input = $ARGV[0];

$inventory = $ARGV[1];


# usage: 	perl transform-TWSI.pl TWSI-contexts TWSI-sense-inventory 
# e.g:		perl ./utils/transform-TWSI.pl ./data/TWSI-2.0-all-sentences.txt ./data/TWSI-2.0-sense-inventory.txt



# file read
open FILE, "$inventory" or die "Can't open file $inventory: $!\n";


# store TWSI senses
while (<FILE>) {  
	chomp;
	@line = split(/\t/,$_);
	$related_words{$line[0]} = $line[1];
}
close FILE;





# file read
open FILE, "$input" or die "Can't open file $input: $!\n";


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
	
	
	print "$line[3]\t$line[1]\tn\t$start,$end\t$line[0]\t\t$related_words{$line[0]}\t\t$context\n";

}
close FILE;


