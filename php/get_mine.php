<?php
require "connection.php";

$mysql_qry = "select lat, lng from mine";
$result = $connection->query($mysql_qry);

$dbdata = array();

while ( $row = $result->fetch_assoc())  {
    $dbdata[]=$row;
}

echo json_encode($dbdata, JSON_NUMERIC_CHECK);

$connection->close();


?>
