# DynamicData for Kotlin

[![Download](https://img.shields.io/bintray/v/magentaize/dynamicdata/dynamicdata?label=Download&logo=jfrog-bintray&color=blue&style=flat-square)](https://bintray.com/magentaize/dynamicdata/dynamicdata/_latestVersion)

DynamicData for Kotlin is a portable class library which brings the power of Reactive Extensions (Rx) to collections written in Kotlin.

You can get details from the original project: [DynamicData](https://github.com/reactivemarbles/DynamicData). 

# API differences

## Interface
|   C#   |  Kotlin  |
|  :----:  | :----:  |
| IChangeSet  | ChangeSet |
| IExtendedList  | ExtendedList |
|IGroup|Group|
|IGrouping|MutableGroup|
|IObservableList|ObservableList|
|IPageChangeSet|PageChangeSet|
|ISourceList|EditableObservableList|

## Class
|C#|Kotlin|
|:----:|:----:|
|ChangeSet|AnonymousChangeSet|

## Operator
|C#|Kotlin|
|:----:|:----:|
|AddRange|addAll|
|RemoveRange|removeAll|
|GroupOn|groupOnMutable|
|GroupOnImmutable|groupOn|
|Filter|filterItem|
|Transform|transform<br/>transformWithIndex<br/>transformWithOptional|

