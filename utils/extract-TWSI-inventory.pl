
# take only polysemous words
$poly = 0;



# file list as arguments
foreach $in (@ARGV) {

	my $word;
	%twords = {};
	%tweights = {};

	# read TWSI inventory file
	open TWSI, $in or die "Can't open file $in: $!\n";
	$maxid = 0;
	while (<TWSI>) {  
		chomp;
		@line = split(/\t/,$_);
		if (@line > 1) {
			$id = (split(/\@\@/, $line[0]))[1];
			if ($id =~ m/\-/) {next;}
			if ($id > $maxid){$maxid=$id};
			$word = $line[1];

			if ($line[2] =~ m/\-\-/ || $line[2] =~ m/^\-$/ || $line[2] =~ m/\"/) {next;} 
			
			if (!exists $twords{$id}) {
				$twords{$id} = $line[2].":".$line[3];
			} else {
				$twords{$id} .= ", $line[2]:$line[3]";
			};
		}	
	}
	if ($poly && ($maxid == 1)) {next;}

	close TWSI;


	foreach $tid (sort keys (%twords)) {
		if ($tid !~ "HASH") {
			print "$word\@\@$tid\t";
			
			%scores = {};
			@subst = split(/,\s+/,$twords{$tid});
			foreach $s (@subst) {
				@kv = split(/:/,$s);
				if (!exists $scores{$kv[0]}) {
					$scores{$kv[0]} = $kv[1];
				} else {
					$scores{$kv[0]} = $scores{$kv[0]} + $kv[1];						
				}
			}
			
			$substitutions = "";
			foreach my $s (sort { $scores{$b} <=> $scores{$a} } keys %scores) {
				if ($s !~ m/HASH/) { 
					$substitutions .= "$s:$scores{$s}, "
				}
			}
				
			print substr($substitutions, 0, -2)."\n";
		}
	}



}

