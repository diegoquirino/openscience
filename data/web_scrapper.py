import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin


class WebScraper:
    """
    A class to scrape all URLs from a given base URL.

    Attributes:
    -----------
    base_url : str
        The base URL from which to start scraping.
    visited : set
        A set to keep track of visited URLs.
    all_urls : set
        A set to store all found URLs.
    """

    def __init__(self, base_url):
        """
        Initializes the WebScraper with the base URL.

        Parameters:
        -----------
        base_url : str
            The base URL from which to start scraping.
        """
        self.base_url = base_url
        self.visited = set()
        self.all_urls = set()

    def scrape_page(self, url):
        """
        Recursively scrapes all URLs on the given page and subpages.

        Parameters:
        -----------
        url : str
            The URL to scrape.
        """
        if url in self.visited:
            return

        # Mark the URL as visited
        self.visited.add(url)

        try:
            # Send a GET request to the URL
            response = requests.get(url)
            response.raise_for_status()  # Raise an error for bad status codes
        except requests.exceptions.RequestException as e:
            # Print error and return if the request fails
            print(f'Error fetching {url}: {e}')
            return

        # Parse the page content with BeautifulSoup
        soup = BeautifulSoup(response.text, 'html.parser')

        # Add the current URL to the set of all URLs
        self.all_urls.add(url)

        # Find and follow all subpage links
        for link in soup.find_all('a', href=True):
            subpage_url = urljoin(url, link['href'])
            # Only follow links that are part of the base URL and haven't been visited
            if subpage_url.startswith(self.base_url) and subpage_url not in self.visited:
                self.scrape_page(subpage_url)

    def get_all_urls(self):
        """
        Start the scraping process and return all found URLs in a tuple format.

        Returns:
        --------
        tuple:
            A tuple of all URLs found under the base URL.
        """
        # Start scraping from the base URL
        self.scrape_page(self.base_url)

        # Return all collected URLs as a tuple
        return tuple(self.all_urls)
