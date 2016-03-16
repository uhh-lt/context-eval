# Usage perl random-baseline.pl TWSI-Dataset inventory-file
if (@ARGV < 2) {
	print "Usage: perl random-baseline.pl DATASET INVENTORY\n";
	exit(1);
}

$twsi = $ARGV[0];

$inventory = $ARGV[1];


# read inventory
open CLUST, $inventory or die "Can't open file $inventory: $!\n";
while (<CLUST>) {  
	chomp;	
	@line = split(/\t/,$_);
	$word = $line[0];
	$id = $line[1];	
	$clusters{"$word $id"} = $line[2];
	$cids{$word} .= "$id ";
}
close CLUST;


# read twsi template file
open TWSI, $twsi or die "Can't open file $twsi: $!\n";
while (<TWSI>) {  
	chomp;	
	@line = split(/\t/,$_);
	$word = $line[1];
	$id = $line[0];
	
	@els = split (/ /, $cids{$word});
	$prediction = $els[rand @els]; 
	$cluster = $clusters{"$word $prediction"};
	# keep header
	if ($line[5] =~ m/predict\_sense\_ids/) {$prediction = $line[5]; $cluster = $line[7];}
	
	print "$id\t$word\t$line[2]\t$line[3]\t$line[4]\t$prediction\t$line[6]\t$cluster\t$line[8]\n";
}
close TWSI;
