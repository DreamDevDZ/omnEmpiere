<?php
$output = shell_exec('php -f ./shell/indexer.php -- args reindexall');
echo $output;
?>
