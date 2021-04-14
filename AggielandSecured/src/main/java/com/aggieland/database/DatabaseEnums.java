package com.aggieland.database;

public enum DatabaseEnums {

  SELECT("SELECT"),
  STAR("*"),
  FROM("FROM"),
  WHERE("WHERE"),
  ANY("%");

  private final String value;

  DatabaseEnums(String value) {
    this.value = value;
  }

  String getSeconds() {
    return value;
  }

}