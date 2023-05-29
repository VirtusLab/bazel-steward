# Building the docs site locally

1. Install Ruby 
   - It's available in [Chocolatey](https://community.chocolatey.org "for Windows"), [Homebrew](https://brew.sh "for Mac OS"), or your system's package repository.
2. Run `bundle install`
   - If this command fails due to being unable to install dependencies, you may need to install the right versions manually, e.g. `gem install concurrent-ruby -v 1.1.10`.
3. Run `bundle exec jekyll serve`
   - Don't forget to uncomment the `theme` entry in `_config.yml`. 
   - Don't forget to comment it out before pushing your changes.
4. Go to <http://localhost:4000> and verify that the site is working correctly.