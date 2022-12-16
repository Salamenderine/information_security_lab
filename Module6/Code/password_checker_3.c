#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

int check_password(char* p, int p_size,  char* i, int i_size) {

    int corr_len = 0;
    for (int idx = 0; idx < p_size; idx++){
        corr_len += (p[idx] != '$');
    }

    int check = 1;
    for (int idx=0; idx<i_size; idx++){
        check *= (p[idx + 15 - i_size] == i[idx]);
    }

    return (corr_len == i_size) & (check == 1);

}

//assumptions: password only has small characters [a, z], maximum length is 15 characters
int main (int argc, char* argv[])	{

	if (argc != 3) {
		fprintf(stderr, "Usage: %s <password guess> <output_file>\n", argv[0]);
		exit(EXIT_FAILURE);
	}

	FILE* password_file;
	char password [16] = "\0";
	
	size_t len = 0;
	char* line;
	password_file = fopen ("/home/isl/t2_3/password.txt", "r");

	if (password_file == NULL) {
		perror("cannot open password file\n");
		exit(EXIT_FAILURE);
	}

    int ch;
    for (int i = 0; i<15 &&(ch=getc(password_file) != EOF;)){
        password[i] = ch;
        ++i;
    }
	// fscanf(password_file, "%s", password);

    int is_match = 0;
    is_match = check_password(password, strlen(password), argv[1], strlen(argv[1]));
	
	FILE* output_file;
	output_file = fopen (argv[2], "wb");
	fputc(is_match, output_file);
	fclose(output_file);
	fclose(password_file);
	return 0;
}

