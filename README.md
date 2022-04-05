<h2 align="center">Metadata Tool</h2>
<p align="center">Make a QC report for metadata & sync all metadata to a database (e.g. MySQL/PostgreSQL/SQLite)</p>

<p align="center">
<img alt="GitHub Workflow Status" src="https://img.shields.io/github/workflow/status/yjcyxky/metadata-tool/test-pack-and-release?label=Build Status">
<img src="https://img.shields.io/github/license/yjcyxky/metadata-tool.svg?label=License" alt="License"> 
<a href="https://github.com/yjcyxky/metadata-tool/releases"><img alt="Latest Release" src="https://img.shields.io/github/release/yjcyxky/metadata-tool.svg?label=Latest%20Release"/></a>
</p>

## Features
- [x] AutoDetect table schema based on the metadata tables.
- [x] Sync all metadata tables to a specified database (Such as MySQL/PostgreSQL/SQLite).
- [x] Send a notification on updated status to dingtalk group.
- [ ] Generate Interactive QC report for a set of metadata tables.
- [ ] Validate fields in the metadata tables based on a set of custom-defined and predefined schema, please access the [biodata-validator](https://github.com/yjcyxky/biodata-validator.git) project for more details.
- [ ] Please submit a pull request if you have any ideas.

## Get Started for Users
### Prepare your metadata
Please follow the [`SEQC DataHub`](https://github.com/biominer-lab/seqc-datahub) repo or the [`DataHub`](https://github.com/biominer-lab/datahub) repo to learn how to prepare the metadata.

```
.
├── project
│   └── project_20220226.csv
├── donor
│   └── donor_20220226.csv
├── biospecimen
│   └── biospecimen_20220316.csv
├── reference_materials
│   └── reference_materials_20220226.csv
├── library
│   ├── CBCGA_library_20220304.csv
│   ├── FDUVAZ_4family_library_20220304.csv
│   ├── Pool4project_library_20220304.csv
│   ├── Quartet_library_20220304.csv
│   ├── published_library_20220304.csv
│   └── published_ma_library_20220304.csv
├── sequencing
│   ├── CBCGA_sequencing_20220306.csv
│   ├── FDUVAZ_4family_sequencing_20220306.csv
│   ├── Pool4project_sequencin_202200306.csv
│   ├── Quartet_sequencing_20220306.csv
│   ├── published_ma_sequencing_20220306.csv
│   └── published_sequencing_20220306.csv
├── datafile
│   ├── CBCGA_datafile_20220306.csv
│   ├── FDUVAZ_4family_datafile_20220306.csv
│   ├── GSE47774_datafile_20220306.csv
│   ├── Pool4project_datafile_20220306.csv
│   ├── Quartet_datafile_20220226.csv
│   ├── otherGEO_datafile_20220306.csv
│   └── published_ma_datafile_20220306.csv
```

### How to use it?

``` shell
java -jar metadata-tool-0.1.0-standalone.jar -h
# MetadataTool - For metadata QC & QA.

# Usage: metadata-tool [options]

# Options:
#   -d, --data-dir PATH  Data Directory
#   -v, --version        Show version
#   -D, --debug          Show debug messages
#   -m, --enable-syncdb  Enable sync to database.
#   -n, --enable-notify  Enable notify user by dingtalk
#   -h, --help

# Please refer to the manual page for more information.
```

## Get Started for Developers

### Clone the repo
``` shell
git clone https://github.com/metadata-tool.git
cd metadata-tool
```

### Make an uberjar

``` shell
lein uberjar
```

### Unittest

``` shell
lein test
```

## Contributing

Comming soon...

## License

Copyright © 2022 Jingcheng Yang

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
