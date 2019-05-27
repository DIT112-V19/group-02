<?php
  require "connection.php";
  $lat = $_POST["lat"];
  $lng = $_POST["lng"];
  $mysql_qry = "insert into mine (lat,lng) values ("'.$lat.'","'.$lng.'")";

  if($connection->query($mysql_qry)===TRUE){
    echo "Insert successful";
  }else{
    "Error: " . $mysql_qry . "<br>" . $connection->error;
  }
  $connection->close();
?>
