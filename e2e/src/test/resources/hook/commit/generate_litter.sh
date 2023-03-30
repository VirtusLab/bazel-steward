for dir in rubbish trash garbage
do
  mkdir $dir
  for i in {1..5}
  do
    echo "$i" >$dir/file_$i.txt
  done
done
